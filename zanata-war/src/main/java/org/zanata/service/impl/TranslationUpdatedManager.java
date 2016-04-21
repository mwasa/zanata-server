package org.zanata.service.impl;

import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.deltaspike.core.api.provider.BeanManagerProvider;
import org.zanata.ApplicationConfiguration;
import org.zanata.async.Async;
import org.zanata.common.ContentState;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.PersonDAO;
import org.zanata.dao.TextFlowDAO;
import org.zanata.events.DocumentStatisticUpdatedEvent;
import org.zanata.events.TextFlowTargetStateEvent;
import org.zanata.events.webhook.DocumentStatsEvent;
import org.zanata.model.HDocument;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.WebHook;
import org.zanata.rest.dto.User;
import org.zanata.rest.editor.service.UserService;
import org.zanata.service.TranslationStateCache;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;

import static org.zanata.events.TextFlowTargetStateEvent.TextFlowTargetState;

/**
 * Manager that handles post update of translation. Important:
 * TextFlowTargetStateEvent IS NOT asynchronous, that is why
 * DocumentStatisticUpdatedEvent is used for webhook processes. See
 * {@link org.zanata.events.TextFlowTargetStateEvent} See
 * {@link org.zanata.events.DocumentStatisticUpdatedEvent}
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Named("translationUpdatedManager")
@RequestScoped
@Slf4j
public class TranslationUpdatedManager {

    @Inject
    private TranslationStateCache translationStateCacheImpl;

    @Inject
    private TextFlowDAO textFlowDAO;

    @Inject
    private PersonDAO personDAO;

    @Inject
    private DocumentDAO documentDAO;

    @Inject
    private UserService userService;

    @Inject
    private ApplicationConfiguration applicationConfiguration;

    @Inject
    private Event<DocumentStatisticUpdatedEvent> documentStatisticUpdatedEvent;

    /**
     * This method contains all logic to be run immediately after a Text Flow
     * Target has been successfully translated.
     */
    @Async
    public void textFlowStateUpdated(
            @Observes(during = TransactionPhase.AFTER_SUCCESS)
            TextFlowTargetStateEvent event) {
        translationStateCacheImpl.textFlowStateUpdated(event);
        publishAsyncEvent(event);
        processWebHookEvent(event);
    }

    // Fire asynchronous event
    void publishAsyncEvent(TextFlowTargetStateEvent event) {
        if (BeanManagerProvider.isActive()) {
            Long versionId = event.getProjectIterationId();
            Long documentId = event.getKey().getDocumentId();
            LocaleId localeId = event.getKey().getLocaleId();

            for(TextFlowTargetState state: event.getStates()) {
                int wordCount =
                    textFlowDAO.getWordCount(state.getTextFlowId());
                // TODO PERF: generate one DocumentStatisticUpdatedEvent per
                // TextFlowTargetStateEvent. See
                // DocumentServiceImpl.documentStatisticUpdated
                documentStatisticUpdatedEvent
                    .fire(new DocumentStatisticUpdatedEvent(
                        versionId, documentId, localeId,
                        wordCount, state.getPreviousState(),
                        state.getNewState()));
            }
        }
    }

    void processWebHookEvent(TextFlowTargetStateEvent event) {
        HPerson person = personDAO.findById(event.getActorId());
        if(person == null) {
            return;
        }
        HDocument document = documentDAO.findById(event.getKey().getDocumentId());
        String docId = document.getDocId();
        String versionSlug = document.getProjectIteration().getSlug();
        HProject project = document.getProjectIteration().getProject();
        if (project.getWebHooks().isEmpty()) {
            return;
        }
        String projectSlug = project.getSlug();
        LocaleId localeId = event.getKey().getLocaleId();

        User user = userService.transferToUser(person.getAccount(),
            applicationConfiguration.isDisplayUserEmail());

        Map<ContentState, Integer> contentStates = Maps.newHashMap();
        for (TextFlowTargetState state : event.getStates()) {
            Long tfId = state.getTextFlowId();
            int wordCount = textFlowDAO.getWordCount(tfId);
            Integer previousStateCount = contentStates.get(state.getPreviousState());
            Integer newStateCount = contentStates.get(state.getNewState());

            if (previousStateCount == null) {
                previousStateCount = 0;
            }
            if (newStateCount == null) {
                newStateCount = 0;
            }
            previousStateCount -= wordCount;
            newStateCount += wordCount;

            contentStates.put(state.getPreviousState(), previousStateCount);
            contentStates.put(state.getNewState(), newStateCount);
        }
        DocumentStatsEvent webhookEvent =
            new DocumentStatsEvent(user, projectSlug,
                versionSlug, docId, localeId, contentStates);

        publishWebhookEvent(project.getWebHooks(), webhookEvent);
    }

    public void publishWebhookEvent(List<WebHook> webHooks,
            DocumentStatsEvent event) {
        for (WebHook webHook : webHooks) {
            WebHooksPublisher.publish(webHook.getUrl(), event,
                    Optional.fromNullable(webHook.getSecret()));
        }
    }

    @VisibleForTesting
    public void init(TranslationStateCache translationStateCacheImpl,
        TextFlowDAO textFlowDAO,
        Event<DocumentStatisticUpdatedEvent> documentStatisticUpdatedEvent,
        DocumentDAO documentDAO,
        PersonDAO personDAO, UserService userService,
        ApplicationConfiguration applicationConfiguration) {
        this.translationStateCacheImpl = translationStateCacheImpl;
        this.textFlowDAO = textFlowDAO;
        this.documentStatisticUpdatedEvent =
            documentStatisticUpdatedEvent;
        this.documentDAO = documentDAO;
        this.personDAO = personDAO;
        this.userService = userService;
        this.applicationConfiguration = applicationConfiguration;
    }
}
