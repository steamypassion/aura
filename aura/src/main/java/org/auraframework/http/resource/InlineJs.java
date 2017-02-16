/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.http.resource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.auraframework.annotations.Annotations.ServiceComponent;
import org.auraframework.def.ApplicationDef;
import org.auraframework.def.BaseComponentDef;
import org.auraframework.def.DefDescriptor;
import org.auraframework.def.DefDescriptor.DefType;
import org.auraframework.instance.Component;
import org.auraframework.javascript.PreInitJavascript;
import org.auraframework.service.RenderingService;
import org.auraframework.system.AuraContext;
import org.auraframework.system.AuraContext.Format;
import org.auraframework.system.AuraContext.Mode;
import org.auraframework.throwable.AuraJWTError;
import org.auraframework.throwable.quickfix.QuickFixException;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceComponent
public class InlineJs extends AuraResourceImpl {

    public InlineJs() {
        super("inline.js", Format.JS);
    }

    private RenderingService renderingService;

    private List<PreInitJavascript> preInitJavascripts;

    @Inject
    public void setRenderingService(RenderingService renderingService) {
        this.renderingService = renderingService;
    }

    private <T extends BaseComponentDef> void internalWrite(HttpServletRequest request,
            HttpServletResponse response, DefDescriptor<T> defDescriptor, AuraContext context)
            throws IOException, QuickFixException {

        servletUtilAdapter.checkFrameworkUID(context);

        servletUtilAdapter.setCSPHeaders(defDescriptor, request, response);
        context.setApplicationDescriptor(defDescriptor);
        definitionService.updateLoaded(defDescriptor);

        // Knowing the app, we can do the HTTP headers, so of which depend on
        // the app in play, so we couldn't do this earlier.
        T def = definitionService.getDefinition(defDescriptor);

        if (!context.isTestMode() && !context.isDevMode()) {
            String defaultNamespace = configAdapter.getDefaultNamespace();
            DefDescriptor<?> referencingDescriptor = (defaultNamespace != null && !defaultNamespace.isEmpty())
                    ? definitionService.getDefDescriptor(String.format("%s:servletAccess", defaultNamespace),
                            ApplicationDef.class)
                    : null;
            definitionService.assertAccess(referencingDescriptor, def);
        }

        if (shouldCacheHTMLTemplate(defDescriptor, request, context)) {
            servletUtilAdapter.setLongCache(response);
        } else {
            servletUtilAdapter.setNoCache(response);
        }

        // Prevents Mhtml Xss exploit:
        PrintWriter out = response.getWriter();
        out.write("\n    ");

        Component template = serverService.writeTemplate(context, def, getComponentAttributes(request), out);
        appendPreInitJavascripts(def, context.getMode(), out);
        renderingService.render(template, null, out);
    }

    /**
     * Writes javascript into pre init "beforeFrameworkInit"
     *
     * @param def current application or component
     * @param mode current Mode from AuraContext
     * @param out response writer
     */
    private void appendPreInitJavascripts(BaseComponentDef def, Mode mode, PrintWriter out) {
        if (this.preInitJavascripts != null && !this.preInitJavascripts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (PreInitJavascript js : this.preInitJavascripts) {
                if (js.shouldInsert(def, mode)) {
                    String code = js.getJavascriptCode(def, mode);
                    if (code != null && !code.isEmpty()) {
                        sb.append(String.format("window.Aura.beforeFrameworkInit.push(function() { %s ; }); ", code));
                    }
                }
            }
            if (sb.length() > 0) {
                String output = String.format(";(function() { window.Aura = window.Aura || {}; window.Aura.beforeFrameworkInit = Aura.beforeFrameworkInit || []; %s }());", sb.toString());
                out.append(output);
            }
        }
    }

    private boolean shouldCacheHTMLTemplate(DefDescriptor<? extends BaseComponentDef> appDefDesc,
            HttpServletRequest request, AuraContext context) throws QuickFixException {
        if (appDefDesc != null && appDefDesc.getDefType().equals(DefType.APPLICATION)) {
            Boolean isOnePageApp = ((ApplicationDef)definitionService.getDefinition(appDefDesc)).isOnePageApp();
            if (isOnePageApp != null) {
                return isOnePageApp;
            }
        }
        return !manifestUtil.isManifestEnabled(request);
    }

    @Override
    public void write(HttpServletRequest request, HttpServletResponse response, AuraContext context)
            throws IOException {
        try {
            // For appcached apps, inline is not expected to return a CSRF token
            if (!manifestUtil.isManifestEnabled()) {
                String token = request.getParameter("jwt");
                if (!configAdapter.validateBootstrap(token)) {
                    throw new AuraJWTError("Invalid jwt parameter");
                }
            }
            DefDescriptor<? extends BaseComponentDef> appDefDesc = context.getLoadingApplicationDescriptor();
            internalWrite(request, response, appDefDesc, context);
        } catch (Throwable t) {
            if (t instanceof AuraJWTError) {
                // If jwt validation fails, just 404. Do not gack.
                try {
                    servletUtilAdapter.send404(request.getServletContext(), request, response);
                } catch (ServletException e) {
                    // ignore
                }
            } else {
                servletUtilAdapter.handleServletException(t, false, context, request, response, false);
                exceptionAdapter.handleException(new AuraResourceException(getName(), response.getStatus(), t));
            }
        }
    }

    @Autowired(required = false) // only clean way to allow no bean vs using Optional
    public void setPreInitJavascripts(List<PreInitJavascript> preInitJavascripts) {
        this.preInitJavascripts = preInitJavascripts;
    }
}
