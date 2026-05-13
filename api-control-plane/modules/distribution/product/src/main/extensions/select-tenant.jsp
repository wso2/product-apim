<%--
  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).

  WSO2 LLC. licenses this file to you under the Apache License,
  Version 2.0 (the "License"); you may not use this file except
  in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%
    String sessionDataKey = request.getParameter("sessionDataKey");
    if (sessionDataKey == null) {
        sessionDataKey = "";
    }
    String authenticator = request.getParameter("authenticator");
    if (authenticator == null) {
        authenticator = "";
    }
    String idp = request.getParameter("idp");
    if (idp == null) {
        idp = "";
    }
    // Use relative URLs so the form/API target stays same-origin regardless of
    // Host header value. Prevents open redirect via spoofed Host header.
    String commonAuthUrl = "/commonauth";
    String tenantsApiUrl = "/api/am/devportal/v3/tenants?state=active";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Select Organization - WSO2 API Manager</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background-color: #f5f6f8;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            color: #333;
        }

        .container {
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
            padding: 40px;
            width: 100%;
            max-width: 420px;
        }

        .logo {
            text-align: center;
            margin-bottom: 24px;
        }

        .logo img {
            height: 40px;
        }

        .logo h2 {
            color: #ff7300;
            font-size: 18px;
            margin-top: 8px;
        }

        h1 {
            font-size: 22px;
            font-weight: 600;
            text-align: center;
            margin-bottom: 8px;
            color: #1a1a1a;
        }

        .subtitle {
            text-align: center;
            color: #666;
            font-size: 14px;
            margin-bottom: 28px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        label {
            display: block;
            font-size: 14px;
            font-weight: 500;
            margin-bottom: 6px;
            color: #444;
        }

        select {
            width: 100%;
            padding: 10px 14px;
            font-size: 14px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            outline: none;
            background-color: #fff;
            appearance: auto;
            transition: border-color 0.2s;
            color: #333;
        }

        select:focus {
            border-color: #ff7300;
            box-shadow: 0 0 0 3px rgba(255, 115, 0, 0.1);
        }

        select:disabled {
            background-color: #f9fafb;
            color: #9ca3af;
            cursor: not-allowed;
        }

        .btn-submit {
            width: 100%;
            padding: 12px;
            font-size: 15px;
            font-weight: 600;
            color: #fff;
            background-color: #ff7300;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            transition: background-color 0.2s;
        }

        .btn-submit:hover {
            background-color: #e66800;
        }

        .btn-submit:disabled {
            background-color: #ffc18a;
            cursor: not-allowed;
        }

        .error-message {
            color: #dc2626;
            font-size: 13px;
            margin-top: 4px;
            display: none;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <h2>WSO2 API Manager</h2>
        </div>

        <h1>Select Your Tenant</h1>
        <p class="subtitle">Select the tenant domain to continue</p>

        <form id="tenantForm" action="<%= Encode.forHtmlAttribute(commonAuthUrl) %>" method="POST">
            <input type="hidden" name="sessionDataKey" value="<%= Encode.forHtmlAttribute(sessionDataKey) %>" />
            <input type="hidden" name="authenticator" value="<%= Encode.forHtmlAttribute(authenticator) %>" />
            <input type="hidden" name="idp" value="<%= Encode.forHtmlAttribute(idp) %>" />

            <div class="form-group">
                <label for="tenantIdentifier">Tenant Domain</label>
                <select id="tenantIdentifier" name="tenantIdentifier" required disabled>
                    <option value="">Loading tenants...</option>
                </select>
                <div class="error-message" id="errorMsg">Please select a tenant domain.</div>
            </div>

            <button type="submit" class="btn-submit" id="submitBtn">Continue</button>
        </form>
    </div>

    <script>
        var TENANTS_API_URL = '<%= Encode.forJavaScript(tenantsApiUrl) %>';

        function fetchTenants() {
            var select = document.getElementById('tenantIdentifier');
            fetch(TENANTS_API_URL)
                .then(function(res) {
                    if (!res.ok) {
                        throw new Error('Tenant API request failed: ' + res.status);
                    }
                    return res.json();
                })
                .then(function(data) {
                    var tenants = (data.list || [])
                        .sort(function(a, b) { return a.domain > b.domain ? 1 : -1; });
                    select.innerHTML = '<option value="">-- Select a tenant --</option>';
                    tenants.forEach(function(t) {
                        var opt = document.createElement('option');
                        opt.value = t.domain;
                        opt.textContent = t.domain;
                        select.appendChild(opt);
                    });
                    select.disabled = false;
                    select.focus();
                })
                .catch(function() {
                    // Fallback: allow manual text entry if API is unreachable.
                    var input = document.createElement('input');
                    input.type = 'text';
                    input.id = 'tenantIdentifier';
                    input.name = 'tenantIdentifier';
                    input.placeholder = 'e.g., abc.com';
                    input.required = true;
                    input.autocomplete = 'off';
                    input.autofocus = true;
                    select.parentNode.replaceChild(input, select);
                    input.focus();
                });
        }

        document.addEventListener('DOMContentLoaded', fetchTenants);

        document.getElementById('tenantForm').addEventListener('submit', function(e) {
            var tenantField = document.getElementById('tenantIdentifier');
            var tenantIdentifier = tenantField.value.trim();
            tenantField.value = tenantIdentifier;
            var errorMsg = document.getElementById('errorMsg');
            var submitBtn = document.getElementById('submitBtn');

            if (!tenantIdentifier) {
                e.preventDefault();
                errorMsg.style.display = 'block';
                return;
            }

            errorMsg.style.display = 'none';

            // Disable button to prevent double submit.
            submitBtn.disabled = true;
            submitBtn.textContent = 'Redirecting...';
        });

        document.getElementById('tenantForm').addEventListener('input', function(e) {
            if (e.target && e.target.id === 'tenantIdentifier') {
                document.getElementById('errorMsg').style.display = 'none';
            }
        });
    </script>
</body>
</html>
