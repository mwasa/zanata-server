/*
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.zanata.common.GlossarySortField;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.zanata.common.LocaleId;
import org.zanata.jpa.FullText;
import org.zanata.model.HGlossaryEntry;
import org.zanata.model.HGlossaryTerm;
import org.zanata.model.HLocale;
import org.zanata.webtrans.shared.rpc.HasSearchType.SearchType;

/**
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 *
 **/
@Named("glossaryDAO")
@RequestScoped
public class GlossaryDAO extends AbstractDAOImpl<HGlossaryEntry, Long> {
    @Inject @FullText
    private FullTextEntityManager entityManager;

    public GlossaryDAO() {
        super(HGlossaryEntry.class);
    }

    public GlossaryDAO(Session session) {
        super(HGlossaryEntry.class, session);
    }

    public HGlossaryEntry getEntryById(Long id) {
        return (HGlossaryEntry) getSession().load(HGlossaryEntry.class, id);
    }

    public int getGlobalEntriesCount(LocaleId srcLocale, String filter) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("select count(term.glossaryEntry) from HGlossaryTerm as term ")
            .append("where term.glossaryEntry.srcLocale.localeId =:srcLocale ")
            .append(
                "and term.locale.localeId = term.glossaryEntry.srcLocale.localeId ")
            .append("and term.glossaryEntry.glossary.global =:isGlobal");

        if(!StringUtils.isBlank(filter)) {
            queryString.append(" and lower(term.content) like lower(:filter)");
        }

        Query query = getSession().createQuery(queryString.toString())
            .setParameter("srcLocale", srcLocale)
            .setParameter("isGlobal", true)
            .setCacheable(true)
            .setComment("GlossaryDAO.getGlobalEntriesCount");

        if(!StringUtils.isBlank(filter)) {
            query.setParameter("filter", "%" + filter + "%");
        }
        Long totalCount = (Long) query.uniqueResult();
        return totalCount == null ? 0 : totalCount.intValue();
    }

    public List<HGlossaryEntry> getGlobalEntriesByLocale(LocaleId srcLocale,
        int offset, int maxResults, String filter,
        List<GlossarySortField> sortFields) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("select term.glossaryEntry from HGlossaryTerm as term ")
            .append("where term.glossaryEntry.srcLocale.localeId =:srcLocale ")
            .append("and term.locale.localeId = term.glossaryEntry.srcLocale.localeId ")
            .append("and term.glossaryEntry.glossary.global =:isGlobal");

        if(!StringUtils.isBlank(filter)) {
            queryString.append(" and lower(term.content) like lower(:filter)");
        }

        if(sortFields!= null && !sortFields.isEmpty()) {
            queryString.append(" ORDER BY ");
            List<String> sortQuery = Lists.newArrayList();
            for(GlossarySortField sortField: sortFields) {
                String order = sortField.isAscending() ? " ASC" : " DESC";
                sortQuery.add(sortField.getEntityField() + order);
            }
            queryString.append(Joiner.on(", ").join(sortQuery));
        }

        Query query = getSession().createQuery(queryString.toString())
            .setParameter("srcLocale", srcLocale)
            .setParameter("isGlobal", true)
            .setCacheable(true)
            .setComment("GlossaryDAO.getGlobalEntriesByLocale");

        if(!StringUtils.isBlank(filter)) {
            query.setParameter("filter", "%" + filter + "%");
        }

        query.setFirstResult(offset).setMaxResults(maxResults);
        return query.list();
    }

    public int getGlobalEntryCountBySourceLocales(LocaleId localeId) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select count(*) from HGlossaryEntry e ")
            .append("where e.srcLocale.localeId = :localeId ")
            .append("and e.glossary.global = :isGlobal ");
        Query query = getSession()
            .createQuery(queryBuilder.toString())
            .setCacheable(true)
            .setParameter("localeId", localeId)
            .setParameter("isGlobal", true)
            .setComment("GlossaryDAO.getGlobalEntryCountBySourceLocales");

        Long totalCount = (Long) query.uniqueResult();
        if (totalCount == null)
            return 0;
        return totalCount.intValue();
    }

    public Map<LocaleId, Integer> getGlobalTranslationLocales(LocaleId srcLocale) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select t.locale, count(*) from HGlossaryTerm t ")
                .append("where t.locale.localeId <> t.glossaryEntry.srcLocale.localeId ")
                .append("and t.glossaryEntry.srcLocale.localeId =:srcLocale ")
                .append("and t.glossaryEntry.glossary.global =:isGlobal ")
                .append("group by t.locale");
        
        Query query = getSession().createQuery(queryBuilder.toString())
            .setParameter("srcLocale", srcLocale)
            .setParameter("isGlobal", true)
            .setComment("GlossaryDAO.getGlobalTranslationLocales");

        @SuppressWarnings("unchecked")
        List<Object[]> list = query.list();
        return generateLocaleStats(list);
    }

    /**
     * Returns map of statistics group by locale id.
     * Object[0] - HLocale
     * Object[1] - Integer word count
     */
    private Map<LocaleId, Integer> generateLocaleStats(List<Object[]> list) {
        Map<LocaleId, Integer> localeStats = Maps.newHashMap();
        for (Object[] obj : list) {
            HLocale locale = (HLocale) obj[0];
            Long count = (Long) obj[1];
            int countInt = count == null ? 0 : Math.toIntExact(count);
            localeStats.put(locale.getLocaleId(), countInt);
        }
        return localeStats;
    }

    public HGlossaryTerm getTermByGlobalEntryAndLocale(Long glossaryEntryId,
            LocaleId locale) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("from HGlossaryTerm as t ")
                .append("WHERE t.locale.localeId =:locale ")
                .append("AND t.glossaryEntry.id= :glossaryEntryId ")
                .append("AND t.glossaryEntry.glossary.global =:isGlobal");
        
        Query query = getSession().createQuery(queryBuilder.toString())
                .setParameter("locale", locale)
                .setParameter("isGlobal", true)
                .setParameter("glossaryEntryId", glossaryEntryId)
                .setComment("GlossaryDAO.getTermByGlobalEntryAndLocale");
        return (HGlossaryTerm) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<HGlossaryTerm> getTermByGlobalEntryId(Long entryId) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("from HGlossaryTerm as t ")
                .append("WHERE t.glossaryEntry.id= :entryId ")
                .append("and t.glossaryEntry.glossary.global =:isGlobal");
        
        Query query = getSession().createQuery(queryBuilder.toString())
                .setParameter("entryId", entryId)
                .setParameter("isGlobal", true)
                .setComment("GlossaryDAO.getTermByGlobalEntryId");
        return query.list();
    }

    public HGlossaryEntry getGlobalEntryByContentHash(String contentHash) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("from HGlossaryEntry as e ")
            .append("WHERE e.contentHash =:contentHash ")
            .append("AND e.glossary.global =:isGlobal");
        
        Query query = getSession().createQuery(queryBuilder.toString())
                .setParameter("contentHash", contentHash)
                .setParameter("isGlobal", true);
        query.setComment("GlossaryDAO.getGlobalEntryByContentHash");
        return (HGlossaryEntry) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<HGlossaryTerm> findTermByIdList(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return Lists.newArrayList();
        }
        Query query =
                getSession().createQuery(
                        "FROM HGlossaryTerm WHERE id in (:idList)");
        query.setParameterList("idList", idList)
                .setCacheable(false)
                .setComment("GlossaryDAO.findTermByIdList");
        return query.list();
    }

    /**
     * Perform lucene search in HGlossaryTerm in srcLocale
     * Object[0] - Float score
     * Object[1] - HGlossaryTerm srcTerm
     */
    public List<Object[]> getSearchResult(String searchText,
            SearchType searchType, LocaleId srcLocale, final int maxResult)
            throws ParseException {
        String queryText;
        switch (searchType) {
        case RAW:
            queryText = searchText;
            break;
        case FUZZY:
            // search by N-grams
            queryText = QueryParser.escape(searchText);
            break;
        case EXACT:
            queryText = "\"" + QueryParser.escape(searchText) + "\"";
            break;
        default:
            throw new RuntimeException("Unknown query type: " + searchType);
        }
        if (StringUtils.isEmpty(queryText)) {
            return Lists.newArrayList();
        }
        QueryParser parser =
                new QueryParser(Version.LUCENE_29, "content",
                        new StandardAnalyzer(Version.LUCENE_29));
        org.apache.lucene.search.Query textQuery = parser.parse(queryText);
        FullTextQuery ftQuery =
                entityManager.createFullTextQuery(textQuery,
                        HGlossaryTerm.class);
        ftQuery.enableFullTextFilter("glossaryLocaleFilter").setParameter(
                "locale", srcLocale);
        ftQuery.setProjection(FullTextQuery.SCORE, FullTextQuery.THIS);
        @SuppressWarnings("unchecked")
        List<Object[]> matches =
                ftQuery.setMaxResults(maxResult).getResultList();
        return matches;
    }

    public int deleteAllGlobalEntries() {
        Query query = getSession().createQuery("Delete HGlossaryTerm where glossaryEntry.glossary.global =: isGlobal");
        query.setParameter("isGlobal", true)
                .setComment("GlossaryDAO.deleteAllGlobalEntries-terms");
        int rowCount = query.executeUpdate();

        Query query2 = getSession().createQuery("Delete HGlossaryEntry where glossary.global =: isGlobal");
        query2.setParameter("isGlobal", true)
                .setComment("GlossaryDAO.deleteAllGlobalEntries-entries");
        query2.executeUpdate();

        return rowCount;
    }
}
