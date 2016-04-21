package org.zanata.events;

import java.util.Map;

import org.zanata.common.ContentState;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@AllArgsConstructor
@Getter
public class DocStatsEvent {
    private final DocumentLocaleKey key;

    /**
     * Updated content states with word counts
     */
    private final Map<ContentState, Integer> contentStates;

    private Long lastModifiedPersonId;
}
