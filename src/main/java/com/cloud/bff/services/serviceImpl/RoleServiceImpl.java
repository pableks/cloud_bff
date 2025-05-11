package com.cloud.bff.services.serviceImpl;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.RoleModel;
import com.cloud.bff.services.RoleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class RoleServiceImpl implements RoleService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl = "https://apiazuregraphqlresttouserwin.azurewebsites.net/api";
    private final String authCode = "byXgwrjxzOwXSB9xP5sdHT76UuFsw_GqwkbHnpun2hDVAzFu6ixNXw==";

    public RoleServiceImpl(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseModel getRoles() {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Create proper GraphQL request object
            Map<String, String> graphqlRequest = new HashMap<>();
            graphqlRequest.put("query", "{ getAllRoles { id title description } }");
            
            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(graphqlRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode rolesNode = rootNode.path("data").path("getAllRoles");
                
                List<RoleModel> roles = new ArrayList<>();
                if (rolesNode.isArray()) {
                    for (JsonNode roleNode : rolesNode) {
                        RoleModel role = new RoleModel();
                        role.setId(Long.parseLong(roleNode.path("id").asText()));
                        role.setTitle(roleNode.path("title").asText());
                        role.setDescription(roleNode.path("description").asText());
                        roles.add(role);
                    }
                }
                
                responseModel.setData(roles);
                responseModel.setMessage("Success");
                responseModel.setStatus(200);
                responseModel.setError(null);
            } catch (JsonProcessingException e) {
                responseModel.setData(graphqlResponse);
                responseModel.setMessage("Error parsing GraphQL response");
                responseModel.setStatus(500);
                responseModel.setError(e.getMessage());
            }

            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel getRoleById(Long id) {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Create proper GraphQL request object
            Map<String, String> graphqlRequest = new HashMap<>();
            graphqlRequest.put("query", "{ getRoleById(id: " + id + ") { id title description } }");

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(graphqlRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode roleNode = rootNode.path("data").path("getRoleById");
                
                if (!roleNode.isMissingNode()) {
                    RoleModel role = new RoleModel();
                    role.setId(Long.parseLong(roleNode.path("id").asText()));
                    role.setTitle(roleNode.path("title").asText());
                    role.setDescription(roleNode.path("description").asText());
                    
                    responseModel.setData(role);
                    responseModel.setMessage("Success");
                    responseModel.setStatus(200);
                    responseModel.setError(null);
                } else {
                    responseModel.setData(null);
                    responseModel.setMessage("Role not found");
                    responseModel.setStatus(404);
                    responseModel.setError("No role found with id: " + id);
                }
            } catch (JsonProcessingException e) {
                responseModel.setData(graphqlResponse);
                responseModel.setMessage("Error parsing GraphQL response");
                responseModel.setStatus(500);
                responseModel.setError(e.getMessage());
            }

            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel createRole(RoleModel role) {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Align with Azure Function's RoleMutationResolver: direct arguments
            String mutation = String.format(
                "mutation { createRole(title: \"%s\", description: \"%s\") }",
                role.getTitle(), 
                role.getDescription()
            );
            
            Map<String, String> graphqlRequest = new HashMap<>();
            graphqlRequest.put("query", mutation);

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(graphqlRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response, expecting a boolean
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode createRoleNode = rootNode.path("data").path("createRole");
                
                if (!createRoleNode.isMissingNode() && createRoleNode.isBoolean() && createRoleNode.asBoolean()) {
                    responseModel.setData(true); // Or role, or null as per preference
                    responseModel.setMessage("Role created successfully");
                    responseModel.setStatus(201);
                    responseModel.setError(null);
                } else {
                    String errorMessage = "Unable to create role.";
                    if (rootNode.has("errors")) {
                        errorMessage = rootNode.get("errors").toString();
                    } else if (!createRoleNode.isMissingNode() && createRoleNode.isBoolean() && !createRoleNode.asBoolean()) {
                        errorMessage = "Create role operation returned false.";
                    }
                    responseModel.setData(graphqlResponse); // Or false
                    responseModel.setMessage("Error creating role");
                    responseModel.setStatus(500);
                    responseModel.setError(errorMessage);
                }
            } catch (JsonProcessingException e) {
                responseModel.setData(graphqlResponse);
                responseModel.setMessage("Error parsing GraphQL response");
                responseModel.setStatus(500);
                responseModel.setError(e.getMessage());
            }
            
            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel updateRole(RoleModel role) {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Align with Azure Function's RoleMutationResolver: direct arguments
            String mutation = String.format(
                "mutation { updateRole(id: \"%s\", title: \"%s\", description: \"%s\") }",
                role.getId(), 
                role.getTitle(), 
                role.getDescription()
            );

            Map<String, String> graphqlRequest = new HashMap<>();
            graphqlRequest.put("query", mutation);

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(graphqlRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response, expecting a boolean
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode updateRoleNode = rootNode.path("data").path("updateRole");
                
                if (!updateRoleNode.isMissingNode() && updateRoleNode.isBoolean() && updateRoleNode.asBoolean()) {
                    responseModel.setData(true); // Or role, or null
                    responseModel.setMessage("Role updated successfully");
                    responseModel.setStatus(200);
                    responseModel.setError(null);
                } else {
                    String errorMessage = "Unable to update role.";
                     if (rootNode.has("errors")) {
                        errorMessage = rootNode.get("errors").toString();
                    } else if (!updateRoleNode.isMissingNode() && updateRoleNode.isBoolean() && !updateRoleNode.asBoolean()) {
                        errorMessage = "Update role operation returned false.";
                    }
                    responseModel.setData(graphqlResponse); // Or false
                    responseModel.setMessage("Error updating role");
                    responseModel.setStatus(500); // Or 404 if ID not found, based on API behavior
                    responseModel.setError(errorMessage);
                }
            } catch (JsonProcessingException e) {
                responseModel.setData(graphqlResponse);
                responseModel.setMessage("Error parsing GraphQL response");
                responseModel.setStatus(500);
                responseModel.setError(e.getMessage());
            }
            
            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel deleteRole(Long id) {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Align with Azure Function's RoleMutationResolver: direct argument
            String mutation = String.format("mutation { deleteRole(id: \"%s\") }", id);

            Map<String, String> graphqlRequest = new HashMap<>();
            graphqlRequest.put("query", mutation);

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(graphqlRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response, expecting a boolean
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode deleteRoleNode = rootNode.path("data").path("deleteRole");
                
                if (!deleteRoleNode.isMissingNode() && deleteRoleNode.isBoolean() && deleteRoleNode.asBoolean()) {
                    Map<String, Object> deleteSuccessResponse = new HashMap<>();
                    deleteSuccessResponse.put("success", true);
                    deleteSuccessResponse.put("message", "Role deleted successfully");
                    deleteSuccessResponse.put("id", id);
                    responseModel.setData(deleteSuccessResponse); // Or just true
                    responseModel.setMessage("Role deleted successfully");
                    responseModel.setStatus(200);
                    responseModel.setError(null);
                } else {
                    String errorMessage = "Unable to delete role.";
                    if (rootNode.has("errors")) {
                        errorMessage = rootNode.get("errors").toString();
                    } else if (!deleteRoleNode.isMissingNode() && deleteRoleNode.isBoolean() && !deleteRoleNode.asBoolean()) {
                        errorMessage = "Delete role operation returned false.";
                    }
                    responseModel.setData(graphqlResponse); // Or false
                    responseModel.setMessage("Error deleting role");
                    responseModel.setStatus(500); // Or 404 if ID not found
                    responseModel.setError(errorMessage);
                }
            } catch (JsonProcessingException e) {
                responseModel.setData(graphqlResponse);
                responseModel.setMessage("Error parsing GraphQL response");
                responseModel.setStatus(500);
                responseModel.setError(e.getMessage());
            }
            
            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }
}