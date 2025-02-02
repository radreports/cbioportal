package org.cbioportal.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.servlet.http.HttpServletRequest;
import org.cbioportal.service.FrontendPropertiesService;
import org.cbioportal.web.util.HttpRequestUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.cbioportal.service.FrontendPropertiesServiceImpl.FrontendProperty;

@Controller
public class IndexPageController {
    private static final Logger log = LoggerFactory.getLogger(IndexPageController.class);
    
    @Autowired
    private FrontendPropertiesService frontendPropertiesService;
    
    @Autowired
    private HttpRequestUtils requestUtils;
    
    @Value("${authenticate}")
    private String[] authenticate;
    
    @Value("${saml.idp.metadata.entityid:not_defined_in_portalproperties}")
    private String samlIdpEntityId;

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, String> getFrontendProperties(HttpServletRequest request, Authentication authentication) {
        String baseUrl = requestUtils.getBaseUrl(request);
        Map<String, String> properties = frontendPropertiesService.getFrontendProperties();
        properties.put("base_url", baseUrl);
        properties.put("user_email_address", authentication != null ? authentication.getName(): "anonymousUser");
        // TODO: Support skin.user_display_name 
        properties.put("user_display_name", authentication != null ? authentication.getName(): "anonymousUser");
        return properties;
    }
    
    @RequestMapping({"/", "/index", "/index.html", "/study/summary", "/results" })
    public String showIndexPage(HttpServletRequest request, Authentication authentication, Model model)
        throws JsonProcessingException {

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(String.class, new CustomFrontendPropertiesSerializer());
        mapper.registerModule(simpleModule);

        String baseUrl = requestUtils.getBaseUrl(request); 
        JSONObject postData = requestUtils.getPostData(request);
        
        model.addAttribute("propertiesJson", mapper.writeValueAsString(getFrontendProperties(request, authentication)));
        model.addAttribute("frontendUrl", frontendPropertiesService.getFrontendProperty(FrontendProperty.frontendUrl));
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("appVersion", frontendPropertiesService.getFrontendProperty(FrontendProperty.app_version));
        model.addAttribute("postData", postData.isEmpty() ? "undefined" : postData);

        return "index";
    }

    @GetMapping("/config_service")
    public ResponseEntity<?> getConfigService(HttpServletRequest request, Authentication authentication) {
        return ResponseEntity.ok(getFrontendProperties(request, authentication));
    }

    public FrontendPropertiesService getFrontendPropertiesService() {
        return frontendPropertiesService;
    }

    public static class CustomFrontendPropertiesSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase((value))) {
                jsonGenerator.writeBoolean("true".equalsIgnoreCase(value));
            } else if (value != null) {
                jsonGenerator.writeString(value);
            } else {
                jsonGenerator.writeNull();
            }
        }
    }
}
