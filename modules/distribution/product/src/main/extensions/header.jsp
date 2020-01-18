
<%--
  ~ Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
--%>

<!-- localize.jsp MUST already be included in the calling script -->

<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<link rel="icon" href="libs/theme/assets/images/favicon.ico" type="image/x-icon"/>
<link href="libs/theme/wso2-default.min.css" rel="stylesheet">

<title>WSO2 API Manager</title>

<style>
    html, body {
        height: 100%;
    }
    body {
        flex-direction: column;
        display: flex;
    }
    main {
        flex-shrink: 0;
    }
    main.center-segment {
        margin: auto;
        display: flex;
        align-items: center;
    }
    main.center-segment > .ui.container.medium {
        max-width: 450px !important;
    }
    main.center-segment > .ui.container.large {
        max-width: 700px !important;
    }
    main.center-segment > .ui.container > .ui.segment {
        padding: 3rem;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .buttons {
        margin-top: 1em;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .buttons.align-right button,
    main.center-segment > .ui.container > .ui.segment .segment-form .buttons.align-right input {
        margin: 0 0 0 0.25em;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .column .buttons.align-left button.link-button,
    main.center-segment > .ui.container > .ui.segment .segment-form .column .buttons.align-left input.link-button {
        padding: .78571429em 1.5em .78571429em 0;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form {
        text-align: left;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .align-center {
        text-align: center;
    }
    main.center-segment > .ui.container > .ui.segment .segment-form .align-right {
        text-align: right;
    }
    footer {
        padding: 2rem 0;
    }
    body .product-title .product-title-text {
        margin: 0;
    }
    body .center-segment .product-title .product-title-text {
        margin-top: 2em;
        margin-bottom: 1em;
    }
    .ui.menu.fixed.app-header .product-logo {
        padding-left: 0;
    }
    /* Table of content styling */
    main #toc {
        position: sticky;
        top: 93px;
    }
    main .ui.segment.toc {
        padding: 20px;
    }
    main .ui.segment.toc ul.ui.list.nav > li.sub {
        margin-left: 20px;
    }
    main .ui.segment.toc ul.ui.list.nav > li > a {
        color: rgba(0,0,0,.87);
        text-decoration: none;
    }
    main .ui.segment.toc ul.ui.list.nav > li:before {
        content: "\2219";
        font-weight: bold;
        font-size: 1.6em;
        line-height: 0.5em;
        display: inline-block;
        width: 1em;
        margin-left: -0.7em;
    }
    main .ui.segment.toc ul.ui.list.nav > li.sub:before {
        content: "\2192";
        margin-left: -1em;
    }
    main .ui.segment.toc ul.ui.list.nav > li:hover a {
        color: #ff5000;
        text-decoration: none;
    }
    main .ui.segment.toc ul.ui.list.nav > li:hover:before {
        color: #ff5000;
    }
</style>

<script src="libs/jquery_3.4.1/jquery-3.4.1.js"></script>
