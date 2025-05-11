package com.cloud.bff.controllers;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.RoleModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.RoleService;
import com.cloud.bff.services.UserService;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/graphql")
public class GraphQLController {

    private final RoleService roleService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(GraphQLController.class);

    public GraphQLController(final RoleService roleService, final UserService userService, ObjectMapper objectMapper) {
        this.roleService = roleService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseModel handleGraphQL(@RequestBody String requestBody) {
        try {
            logger.info("Received GraphQL request: {}", requestBody);
            
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
            
            // Handle ROLE operations
            if (graphqlQuery.contains("getAllRoles") || graphqlQuery.contains("roles")) {
                logger.info("Processing 'getAllRoles/roles' query");
                return roleService.getRoles();
            } else if (graphqlQuery.contains("getRoleById")) {
                logger.info("Processing 'getRoleById' query");
                try {
                    Long id = extractIdFromGetByIdQuery(graphqlQuery, "getRoleById");
                    logger.info("Extracted ID: {}", id);
                    return roleService.getRoleById(id);
                } catch (Exception e) {
                    logger.error("Error extracting ID from query: {}", e.getMessage());
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(400);
                    errorResponse.setMessage("Invalid ID in GraphQL query");
                    errorResponse.setError("Could not parse ID: " + e.getMessage());
                    return errorResponse;
                }
            } else if (graphqlQuery.contains("role(id:")) {
                logger.info("Processing 'role by id' query");
                try {
                    Long id = extractIdFromQuery(graphqlQuery, "role");
                    logger.info("Extracted ID: {}", id);
                    return roleService.getRoleById(id);
                } catch (Exception e) {
                    logger.error("Error extracting ID from query: {}", e.getMessage());
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(400);
                    errorResponse.setMessage("Invalid ID in GraphQL query");
                    errorResponse.setError("Could not parse ID: " + e.getMessage());
                    return errorResponse;
                }
            } else if (graphqlQuery.contains("createRole")) {
                logger.info("Processing 'createRole' mutation");
                RoleModel role = extractRoleFromMutation(graphqlQuery, "createRole");
                logger.info("Extracted role: title={}, description={}", role.getTitle(), role.getDescription());
                return roleService.createRole(role);
            } else if (graphqlQuery.contains("updateRole")) {
                logger.info("Processing 'updateRole' mutation");
                RoleModel role = extractRoleFromMutation(graphqlQuery, "updateRole");
                logger.info("Extracted role: id={}, title={}, description={}", role.getId(), role.getTitle(), role.getDescription());
                return roleService.updateRole(role);
            } else if (graphqlQuery.contains("deleteRole")) {
                logger.info("Processing 'deleteRole' mutation");
                try {
                    Long id = extractIdFromDeleteMutation(graphqlQuery, "deleteRole");
                    logger.info("Extracted ID for deletion: {}", id);
                    return roleService.deleteRole(id);
                } catch (Exception e) {
                    logger.error("Error extracting ID for deletion: {}", e.getMessage());
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(400);
                    errorResponse.setMessage("Invalid ID in delete mutation");
                    errorResponse.setError("Could not parse ID: " + e.getMessage());
                    return errorResponse;
                }
            }
            
            // Handle USER operations
            else if (graphqlQuery.contains("getAllUsers") || graphqlQuery.contains("users")) {
                logger.info("Processing 'getAllUsers/users' query");
                return userService.getUsers();
            } else if (graphqlQuery.contains("getUserById")) {
                logger.info("Processing 'getUserById' query");
                try {
                    Long id = extractIdFromGetByIdQuery(graphqlQuery, "getUserById");
                    logger.info("Extracted ID: {}", id);
                    return userService.getUserById(id);
                } catch (Exception e) {
                    logger.error("Error extracting ID from query: {}", e.getMessage());
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(400);
                    errorResponse.setMessage("Invalid ID in GraphQL query");
                    errorResponse.setError("Could not parse ID: " + e.getMessage());
                    return errorResponse;
                }
            } else if (graphqlQuery.contains("user(id:")) {
                logger.info("Processing 'user by id' query");
                try {
                    Long id = extractIdFromQuery(graphqlQuery, "user");
                    logger.info("Extracted ID: {}", id);
                    return userService.getUserById(id);
                } catch (Exception e) {
                    logger.error("Error extracting ID from query: {}", e.getMessage());
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(400);
                    errorResponse.setMessage("Invalid ID in GraphQL query");
                    errorResponse.setError("Could not parse ID: " + e.getMessage());
                    return errorResponse;
                }
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
                    Long id = extractIdFromDeleteMutation(graphqlQuery, "deleteUser");
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

    @PostMapping("/debug")
    public ResponseModel debugGraphQL(@RequestBody String graphqlQuery) {
        ResponseModel response = new ResponseModel();
        try {
            // Create diagnostic information
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("receivedQuery", graphqlQuery);
            
            // Debug role queries
            if (graphqlQuery.contains("role(id:")) {
                try {
                    Long id = extractIdFromQuery(graphqlQuery, "role");
                    debugInfo.put("extractedId", id);
                    debugInfo.put("idExtractionSuccess", true);
                    debugInfo.put("entityType", "role");
                } catch (Exception e) {
                    debugInfo.put("idExtractionSuccess", false);
                    debugInfo.put("idExtractionError", e.getMessage());
                    
                    // Add step-by-step debug info
                    int idIndex = graphqlQuery.indexOf("id:");
                    debugInfo.put("idIndex", idIndex);
                    
                    if (idIndex != -1) {
                        // Show characters around the ID position
                        int start = Math.max(0, idIndex - 5);
                        int end = Math.min(graphqlQuery.length(), idIndex + 15);
                        debugInfo.put("contextAroundId", graphqlQuery.substring(start, end));
                    }
                }
            }
            // Debug user queries
            else if (graphqlQuery.contains("user(id:")) {
                try {
                    Long id = extractIdFromQuery(graphqlQuery, "user");
                    debugInfo.put("extractedId", id);
                    debugInfo.put("idExtractionSuccess", true);
                    debugInfo.put("entityType", "user");
                } catch (Exception e) {
                    debugInfo.put("idExtractionSuccess", false);
                    debugInfo.put("idExtractionError", e.getMessage());
                    debugInfo.put("entityType", "user");
                    
                    // Add step-by-step debug info
                    int idIndex = graphqlQuery.indexOf("id:");
                    debugInfo.put("idIndex", idIndex);
                    
                    if (idIndex != -1) {
                        // Show characters around the ID position
                        int start = Math.max(0, idIndex - 5);
                        int end = Math.min(graphqlQuery.length(), idIndex + 15);
                        debugInfo.put("contextAroundId", graphqlQuery.substring(start, end));
                    }
                }
            }
            
            response.setStatus(200);
            response.setMessage("Debug information");
            response.setData(debugInfo);
            return response;
        } catch (Exception e) {
            response.setStatus(500);
            response.setMessage("Error in debug endpoint");
            response.setError(e.getMessage());
            return response;
        }
    }

    private Long extractIdFromQuery(String query, String entityType) {
        try {
            // Improved regex pattern to extract the ID more reliably
            String idPattern = entityType + "\\(id:\\s*([0-9]+)\\s*\\)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(query);
            
            if (matcher.find()) {
                String idStr = matcher.group(1).trim();
                return Long.parseLong(idStr);
            }
            
            // Fallback to the original method if regex doesn't match
            int startIndex = query.indexOf("id:");
            if (startIndex != -1) {
                startIndex += 3; // Move past "id:"
                
                // Find the closing parenthesis or the next space
                int endIndex = query.indexOf(")", startIndex);
                int spaceIndex = query.indexOf(" ", startIndex);
                int braceIndex = query.indexOf("}", startIndex);
                
                // Take the closest ending marker
                if (spaceIndex != -1 && (endIndex == -1 || spaceIndex < endIndex)) {
                    endIndex = spaceIndex;
                }
                
                if (braceIndex != -1 && (endIndex == -1 || braceIndex < endIndex)) {
                    endIndex = braceIndex;
                }
                
                if (endIndex != -1) {
                    String idStr = query.substring(startIndex, endIndex).trim();
                    // Remove any quotes and other non-numeric characters
                    idStr = idStr.replaceAll("[^0-9]", "").trim();
                    
                    if (!idStr.isEmpty()) {
                        return Long.parseLong(idStr);
                    }
                }
            }
            
            throw new IllegalArgumentException("Could not extract valid ID from GraphQL query: " + query);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing ID from query: " + e.getMessage() + ", Query: " + query);
        }
    }

    private Long extractIdFromGetByIdQuery(String query, String methodName) {
        try {
            // Pattern for getRoleById(id: 1) or getUserById(id: 1) syntax
            String idPattern = methodName + "\\(id:\\s*([0-9]+)\\s*\\)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(query);
            
            if (matcher.find()) {
                String idStr = matcher.group(1).trim();
                return Long.parseLong(idStr);
            }
            
            // Fallback: look for any id field with a number
            idPattern = "id:\\s*([0-9]+)";
            pattern = java.util.regex.Pattern.compile(idPattern);
            matcher = pattern.matcher(query);
            
            if (matcher.find()) {
                String idStr = matcher.group(1).trim();
                return Long.parseLong(idStr);
            }
            
            throw new IllegalArgumentException("Could not extract valid ID from GraphQL query: " + query);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing ID from query: " + e.getMessage() + ", Query: " + query);
        }
    }

    private Long extractIdFromDeleteMutation(String mutation, String methodName) {
        try {
            // Improved regex pattern for delete mutation
            String idPattern = methodName + "\\(id:\\s*\"?([0-9]+)\"?\\s*\\)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(mutation);
            
            if (matcher.find()) {
                String idStr = matcher.group(1).trim();
                return Long.parseLong(idStr);
            }
            
            // Fallback to the original method
            int startIndex = mutation.indexOf(methodName + "(id:");
            if (startIndex != -1) {
                startIndex += (methodName + "(id:").length();
                
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

    private RoleModel extractRoleFromMutation(String mutation, String operationType) {
        // Extract information from the mutation string
        RoleModel role = new RoleModel();
        
        if (operationType.equals("updateRole")) {
            // For update, we need to extract the ID
            String idPattern = "id:\\s*\"?([0-9]+)\"?";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(mutation);
            if (matcher.find()) {
                role.setId(Long.parseLong(matcher.group(1)));
            }
        }
        
        // Extract title
        String titlePattern = "title:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern titlePat = java.util.regex.Pattern.compile(titlePattern);
        java.util.regex.Matcher titleMatcher = titlePat.matcher(mutation);
        if (titleMatcher.find()) {
            role.setTitle(titleMatcher.group(1));
        }
        
        // Extract description
        String descPattern = "description:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern descPat = java.util.regex.Pattern.compile(descPattern);
        java.util.regex.Matcher descMatcher = descPat.matcher(mutation);
        if (descMatcher.find()) {
            role.setDescription(descMatcher.group(1));
        }
        
        return role;
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