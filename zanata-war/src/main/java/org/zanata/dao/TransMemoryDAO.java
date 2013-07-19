/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.dao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.model.tm.TMTransUnitVariant;
import org.zanata.model.tm.TMTranslationUnit;
import org.zanata.model.tm.TransMemory;

/**
 * Data Access Object for Translation Memory and related entities.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("transMemoryDAO")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class TransMemoryDAO extends AbstractDAOImpl<TransMemory, Long>
{
   public TransMemoryDAO()
   {
      super(TransMemory.class);
   }

   public TransMemoryDAO(Session session)
   {
      super(TransMemory.class, session);
   }

   public @Nullable
   TransMemory getBySlug(@Nonnull String slug)
   {
      if(!StringUtils.isEmpty(slug))
      {
         return (TransMemory) getSession().byNaturalId(TransMemory.class).using("slug", slug).load();
      }
      return null;
   }

   public void deleteTransMemoryContents(@Nonnull String slug)
   {
      TransMemory tm = getBySlug(slug);
      tm.getTranslationUnits().clear();
      getSession().update(tm);
   }
}
