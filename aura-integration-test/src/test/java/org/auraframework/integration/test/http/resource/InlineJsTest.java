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
package org.auraframework.integration.test.http.resource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import org.auraframework.adapter.ConfigAdapter;
import org.auraframework.adapter.ServletUtilAdapter;
import org.auraframework.def.ApplicationDef;
import org.auraframework.def.ComponentDef;
import org.auraframework.def.DefDescriptor;
import org.auraframework.http.resource.InlineJs;
import org.auraframework.impl.AuraImplTestCase;
import org.auraframework.service.ContextService;
import org.auraframework.service.DefinitionService;
import org.auraframework.service.InstanceService;
import org.auraframework.service.LoggingService;
import org.auraframework.service.RenderingService;
import org.auraframework.service.ServerService;
import org.auraframework.system.AuraContext;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class InlineJsTest extends AuraImplTestCase {

    @Inject
    private ContextService contextService;

    @Inject
    private ConfigAdapter configAdapter;

    @Inject
    private DefinitionService definitionService;

    @Inject
    private LoggingService loggingService;

    @Inject
    private RenderingService renderingService;

    @Inject
    private InstanceService instanceService;

    @Inject
    private ServletUtilAdapter servletUtilAdapter;

    @Inject
    private ServerService serverService;

    private InlineJs getInlineJs() {
        InlineJs inlineJs = new InlineJs();
        inlineJs.setServletUtilAdapter(servletUtilAdapter);
        inlineJs.setConfigAdapter(configAdapter);
        inlineJs.setLoggingService(loggingService);
        inlineJs.setDefinitionService(definitionService);
        inlineJs.setInstanceService(instanceService);
        inlineJs.setContextService(contextService);
        inlineJs.setServerService(serverService);
        inlineJs.setRenderingService(renderingService);
        return inlineJs;
    }

    @Test
    public void testInlineScriptIsWritenIntoInlineJs() throws Exception {
        // Arrange
        if (contextService.isEstablished()) {
            contextService.endContext();
        }

        String script = "var foo = null;";
        String templateMarkup = String.format(baseComponentTag, "isTemplate='true'", String.format("<script>%s</script>", script));
        DefDescriptor<ComponentDef> templateDesc = addSourceAutoCleanup(ComponentDef.class, templateMarkup);
        String appTagAttributes = String.format("template='%s'", templateDesc.getDescriptorName());
        String appMarkup = String.format(baseApplicationTag, appTagAttributes, "");
        DefDescriptor<ApplicationDef> appDesc = addSourceAutoCleanup(ApplicationDef.class, appMarkup);

        AuraContext context = contextService.startContext(
                AuraContext.Mode.DEV, AuraContext.Format.JS, AuraContext.Authentication.AUTHENTICATED, appDesc);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        InlineJs inlineJs = getInlineJs();

        // Act
        inlineJs.write(mockRequest, mockResponse, context);
        String content = mockResponse.getContentAsString();

        // Assert
        assertEquals("Didn't find expected inline scripts in response content.", script, content.trim());
    }

    @Test
    public void testMultpleInlineScriptOnSameTemplate() throws Exception {
        // Arrange
        if (contextService.isEstablished()) {
            contextService.endContext();
        }

        String script = "var foo = null;";
        String templateMarkup = String.format(baseComponentTag, "isTemplate='true'", String.format("<script>%s</script><script>%s</script>", script, script));
        DefDescriptor<ComponentDef> templateDesc = addSourceAutoCleanup(ComponentDef.class, templateMarkup);
        String appTagAttributes = String.format("template='%s'", templateDesc.getDescriptorName());
        String appMarkup = String.format(baseApplicationTag, appTagAttributes, "");
        DefDescriptor<ApplicationDef> appDesc = addSourceAutoCleanup(ApplicationDef.class, appMarkup);

        AuraContext context = contextService.startContext(
                AuraContext.Mode.DEV, AuraContext.Format.JS, AuraContext.Authentication.AUTHENTICATED, appDesc);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        InlineJs inlineJs = getInlineJs();

        // Act
        inlineJs.write(mockRequest, mockResponse, context);
        String content = mockResponse.getContentAsString();

        // Assert
        String expected = script + script;
        String actual = content.trim();
        assertEquals("Didn't find expected inline scripts in response content.", expected, actual);
    }

    @Test
    public void testMultpleInlineScriptOnTemplateInheritance() throws Exception {
        // Arrange
        if (contextService.isEstablished()) {
            contextService.endContext();
        }

        String script = "var foo = null;";
        String baseCmpTagAttributes = "isTemplate='true' extensible='true'";
        String baseTemplateMarkup = String.format(baseComponentTag, baseCmpTagAttributes, String.format("<script>%s</script>{!v.body}", script));
        DefDescriptor<ComponentDef> baseTemplateDesc = addSourceAutoCleanup(ComponentDef.class, baseTemplateMarkup);

        String cmpTagAttributes = String.format("isTemplate='true' extends='%s'", baseTemplateDesc.getDescriptorName());
        String templateMarkup = String.format(baseComponentTag, cmpTagAttributes, String.format("<script>%s</script>", script));
        DefDescriptor<ComponentDef> templateDesc = addSourceAutoCleanup(ComponentDef.class, templateMarkup);

        String appTagAttributes = String.format("template='%s'", templateDesc.getDescriptorName());
        String appMarkup = String.format(baseApplicationTag, appTagAttributes, "");
        DefDescriptor<ApplicationDef> appDesc = addSourceAutoCleanup(ApplicationDef.class, appMarkup);

        AuraContext context = contextService.startContext(
                AuraContext.Mode.DEV, AuraContext.Format.JS, AuraContext.Authentication.AUTHENTICATED, appDesc);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        InlineJs inlineJs = getInlineJs();

        // Act
        inlineJs.write(mockRequest, mockResponse, context);
        String content = mockResponse.getContentAsString();

        // Assert
        String expected = script + script;
        String actual = content.trim();
        assertEquals("Didn't find expected inline scripts in response content.", expected, actual);
    }

    /**
     * Verify all content in script tag in aura:template is written into inlineJs.
     *
     * TODO: go over inline JS in aura:template and split the test into specific small cases.
     */
    public void testInlineScriptInAuraTemplate() throws Exception {
        // Arrange
        if (contextService.isEstablished()) {
            contextService.endContext();
        }

        String appMarkup = String.format(baseApplicationTag, "template='aura:template'", "");
        DefDescriptor<ApplicationDef> appDesc = addSourceAutoCleanup(ApplicationDef.class, appMarkup);

        AuraContext context = contextService.startContext(
                AuraContext.Mode.DEV, AuraContext.Format.JS, AuraContext.Authentication.AUTHENTICATED, appDesc);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        InlineJs inlineJs = getInlineJs();

        // Act
        inlineJs.write(mockRequest, mockResponse, context);
        String content = mockResponse.getContentAsString();

        // Assert
        this.goldFileText(content);
    }

    @Test
    public void testInlineScriptInTemplateWhichExtendsAuraTemplate() throws Exception {
        // Arrange
        if (contextService.isEstablished()) {
            contextService.endContext();
        }

        String script = "var foo = null;";
        String cmpTagAttributes = String.format("isTemplate='true' extends='aura:template'");
        String templateMarkup = String.format(baseComponentTag, cmpTagAttributes, String.format("<script>%s</script>", script));
        DefDescriptor<ComponentDef> templateDesc = addSourceAutoCleanup(ComponentDef.class, templateMarkup);
        String appTagAttributes = String.format("template='%s'", templateDesc.getDescriptorName());
        String appMarkup = String.format(baseApplicationTag, appTagAttributes, "");
        DefDescriptor<ApplicationDef> appDesc = addSourceAutoCleanup(ApplicationDef.class, appMarkup);

        AuraContext context = contextService.startContext(
                AuraContext.Mode.DEV, AuraContext.Format.JS, AuraContext.Authentication.AUTHENTICATED, appDesc);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        InlineJs inlineJs = getInlineJs();

        // Act
        inlineJs.write(mockRequest, mockResponse, context);
        String content = mockResponse.getContentAsString();

        // Assert
        assertThat("Didn't find expected inline scripts in response content.", content, containsString(script));
    }
}
