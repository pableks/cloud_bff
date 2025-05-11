package com.cloud.bff.controllers;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/graphql")
public class UserGraphQLController {

    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(UserGraphQLController.class);

    public UserGraphQLController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseModel handleGraphQL(@RequestBody String requestBody) {
        try {
            logger.info("Received GraphQL request for users: {}", requestBody);
            
            // Try to parse as JSON first (standard GraphQL format)
            String graphqlQuery = "";
            try {
                JsonNode rootNode = objectMapper.readTree(requestBody);
                if (rootNode.has("query")) {
                    String queryValue = rootNode.get("query").asText();
                    try {
                        // Check if the query value is itself JSON
                        JsonNode queryNode = objectMapper.readTree(queryValue);
                        if (queryNode.has("query")) {
                            // We have a nested query object
                            graphqlQuery = queryNode.get("query").asText();
                        } else {
                            // Not a nested object, use the query value directly
                            graphqlQuery = queryValue;
                        }
                    } catch (Exception e) {
                        // The query value is not valid JSON, use it as-is
                        graphqlQuery = queryValue;
                    }
                    logger.info("Parsed GraphQL query from JSON: {}", graphqlQuery);
                } else {
                    // If not in standard format, use the raw body
                    graphqlQuery = requestBody;
                }
            } catch (Exception e) {
                // Not valid JSON, assume it's a raw GraphQL query
                graphqlQuery = requestBody;
                logger.info("Using raw GraphQL query: {}", graphqlQuery);
            }
            
            // Remove any enclosing quotes and escape sequences that might be present
            graphqlQuery = graphqlQuery.trim();
            if (graphqlQuery.startsWith("\"") && graphqlQuery.endsWith("\"")) {
                graphqlQuery = graphqlQuery.substring(1, graphqlQuery.length() - 1);
            }
            graphqlQuery = graphqlQuery.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            
            // After cleaning the graphqlQuery string
            graphqlQuery = graphqlQuery.trim();
            if (!graphqlQuery.toLowerCase().startsWith("query") && !graphqlQuery.toLowerCase().startsWith("mutation")) {
                graphqlQuery = "query " + graphqlQuery;
            }
            logger.info("Final GraphQL query after processing: {}", graphqlQuery);
            
            // Check for different operation types
            if (graphqlQuery.contains("getAllUsers") || graphqlQuery.contains("users")) {
                logger.info("Processing 'getAllUsers' query");
                return userService.getUsers();
            } else if (graphqlQuery.contains("createUser")) {
                logger.info("Processing 'createUser' mutation");
                UserModel user = extractUserFromMutation(graphqlQuery, "createUser");
                logger.info("Extracted user: email={}, rol={}", user.getEmail(), user.getRol());
                return userService.addUser(user);
            } else if (graphqlQuery.contains("updateUser")) {
                logger.info("Processing 'updateUser' mutation");
                UserModel user = extractUserFromMutation(graphqlQuery, "updateUser");
                logger.info("Extracted user: id={}, email={}, rol={}", user.getId(), user.getEmail(), user.getRol());
                return userService.updateUser(user);
            } else if (graphqlQuery.contains("deleteUser")) {
                logger.info("Processing 'deleteUser' mutation");
                try {
                    Long id = extractIdFromDeleteMutation(graphqlQuery);
                    logger.info("Extracted ID for deletion: {}", id);
                    return userService.deleteUser(id);
                } catch (Exception e) {
                    logger.error("Error extracting ID for deletion: {}", e.getMessage());
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(400);
                    errorResponse.setMessage("Invalid ID in delete mutation");
                    errorResponse.setError("Could not parse ID: " + e.getMessage());
                    return errorResponse;
                }
            } else {
                logger.warn("Invalid GraphQL operation: {}", graphqlQuery);
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid GraphQL operation");
                errorResponse.setError("Unsupported operation in GraphQL query");
                return errorResponse;
            }
        } catch (Exception e) {
            logger.error("Error processing GraphQL request: {}", e.getMessage(), e);
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(500);
            errorResponse.setMessage("Error processing GraphQL request");
            errorResponse.setError(e.getMessage());
            return errorResponse;
        }
    }

    private Long extractIdFromDeleteMutation(String mutation) {
        try {
            // Improved regex pattern for delete mutation
            String idPattern = "deleteUser\\(id:\\s*\"?([0-9]+)\"?\\s*\\)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(mutation);
            
            if (matcher.find()) {
                String idStr = matcher.group(1).trim();
                return Long.parseLong(idStr);
            }
            
            // Fallback to the original method
            int startIndex = mutation.indexOf("deleteUser(id:");
            if (startIndex != -1) {
                startIndex += "deleteUser(id:".length();
                
                // Find the closing parenthesis or the next space
                int endIndex = mutation.indexOf(")", startIndex);
                int spaceIndex = mutation.indexOf(" ", startIndex);
                int braceIndex = mutation.indexOf("}", startIndex);
                
                // Take the closest ending marker
                if (spaceIndex != -1 && (endIndex == -1 || spaceIndex < endIndex)) {
                    endIndex = spaceIndex;
                }
                
                if (braceIndex != -1 && (endIndex == -1 || braceIndex < endIndex)) {
                    endIndex = braceIndex;
                }
                
                if (endIndex != -1) {
                    String idStr = mutation.substring(startIndex, endIndex).trim();
                    // Remove any quotes and other non-numeric characters
                    idStr = idStr.replaceAll("[^0-9]", "").trim();
                    
                    if (!idStr.isEmpty()) {
                        return Long.parseLong(idStr);
                    }
                }
            }
            
            throw new IllegalArgumentException("Could not extract valid ID from delete mutation: " + mutation);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing ID from delete mutation: " + e.getMessage() + ", Mutation: " + mutation);
        }
    }

    private UserModel extractUserFromMutation(String mutation, String operationType) {
        // Extract information from the mutation string
        UserModel user = new UserModel();
        
        if (operationType.equals("updateUser")) {
            // For update, we need to extract the ID
            String idPattern = "id:\\s*\"?([0-9]+)\"?";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(mutation);
            if (matcher.find()) {
                user.setId(Long.parseLong(matcher.group(1)));
            }
        }
        
        // Extract email
        String emailPattern = "email:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern emailPat = java.util.regex.Pattern.compile(emailPattern);
        java.util.regex.Matcher emailMatcher = emailPat.matcher(mutation);
        if (emailMatcher.find()) {
            user.setEmail(emailMatcher.group(1));
        }
        
        // Extract password
        String passwordPattern = "password:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern passwordPat = java.util.regex.Pattern.compile(passwordPattern);
        java.util.regex.Matcher passwordMatcher = passwordPat.matcher(mutation);
        if (passwordMatcher.find()) {
            user.setPassword(passwordMatcher.group(1));
        }
        
        // Extract role/rol - try both variants
        String rolPattern = "rol(?:e)?(?:Id)?:\\s*\"?([^\",})]*)\"?";
        java.util.regex.Pattern rolPat = java.util.regex.Pattern.compile(rolPattern);
        java.util.regex.Matcher rolMatcher = rolPat.matcher(mutation);
        if (rolMatcher.find()) {
            user.setRol(rolMatcher.group(1));
        }
        
        return user;
    }
}