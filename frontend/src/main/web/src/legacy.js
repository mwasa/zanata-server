/*
 * Copyright 2016, Red Hat, Inc. and individual contributors as indicated by the
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

import 'babel-polyfill'
import React from 'react'
import { render } from 'react-dom'
import { isUndefined, forEach, mapValues } from 'lodash'
import { Icons } from './components'
import WebFont from 'webfontloader'
import Nav from './components/Nav'
import './styles/base.css'
import './styles/atomic.css'
import './styles/extras.css'
import { isJsonString } from './utils/StringUtils'
/**
 * Root component that display only side menu bar.
 * Used jsf page that only needs side menu bar from frontend.
 */
WebFont.load({
  google: {
    families: [
      'Source Sans Pro:200,400,600',
      'Source Code Pro:400,600'
    ]
  },
  timeout: 2000
})

window.config = mapValues(window.config, (value) =>
  isJsonString(value) ? JSON.parse(value) : value)

const links = {
  'context': window.config.baseUrl,
  '/login': window.config.links.loginUrl,
  '/signup': window.config.links.registerUrl
}

const activePath = window.location.pathname

render(
  <div>
    <Icons />
    <Nav active={activePath} isJsfPage links={links} />
  </div>
  ,
  document.getElementById('root')
)
