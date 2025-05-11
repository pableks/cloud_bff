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
            graphqlRequest.put("query", "{ role(id: " + id + ") { id title description } }");

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
                JsonNode roleNode = rootNode.path("data").path("role");
                
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
            // Raw GraphQL mutation without JSON wrapper
            String mutation = "mutation { createRole(input: {title: \"" + role.getTitle() + 
                              "\", description: \"" + role.getDescription() + 
                              "\"}) { id title description } }";

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mutation)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode createdRoleNode = rootNode.path("data").path("createRole");
                
                if (!createdRoleNode.isMissingNode()) {
                    RoleModel createdRole = new RoleModel();
                    createdRole.setId(Long.parseLong(createdRoleNode.path("id").asText()));
                    createdRole.setTitle(createdRoleNode.path("title").asText());
                    createdRole.setDescription(createdRoleNode.path("description").asText());
                    
                    responseModel.setData(createdRole);
                    responseModel.setMessage("Role created successfully");
                    responseModel.setStatus(201);
                    responseModel.setError(null);
                } else {
                    responseModel.setData(graphqlResponse);
                    responseModel.setMessage("Error creating role");
                    responseModel.setStatus(500);
                    responseModel.setError("Unable to create role");
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
            // Raw GraphQL mutation without JSON wrapper - fixed format based on API requirements
            String mutation = "mutation { updateRole(input: {id: \"" + role.getId() + 
                              "\", title: \"" + role.getTitle() + 
                              "\", description: \"" + role.getDescription() + 
                              "\"}) { id title description } }";

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mutation)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode updatedRoleNode = rootNode.path("data").path("updateRole");
                
                if (!updatedRoleNode.isMissingNode()) {
                    RoleModel updatedRole = new RoleModel();
                    updatedRole.setId(Long.parseLong(updatedRoleNode.path("id").asText()));
                    updatedRole.setTitle(updatedRoleNode.path("title").asText());
                    updatedRole.setDescription(updatedRoleNode.path("description").asText());
                    
                    responseModel.setData(updatedRole);
                    responseModel.setMessage("Role updated successfully");
                    responseModel.setStatus(200);
                    responseModel.setError(null);
                } else {
                    responseModel.setData(graphqlResponse);
                    responseModel.setMessage("Error updating role");
                    responseModel.setStatus(500);
                    responseModel.setError("Unable to update role");
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
            // Raw GraphQL mutation with the correct fields for DeleteResponse
            String mutation = "mutation { deleteRole(id: \"" + id + "\") { success message } }";

            String graphqlResponse = webClient.post()
                    .uri("/graphql")
                    .header("x-functions-key", authCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mutation)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the GraphQL response
            try {
                JsonNode rootNode = objectMapper.readTree(graphqlResponse);
                JsonNode deleteResponseNode = rootNode.path("data").path("deleteRole");
                
                if (!deleteResponseNode.isMissingNode() && deleteResponseNode.path("success").asBoolean()) {
                    Map<String, Object> deleteResponse = new HashMap<>();
                    deleteResponse.put("success", deleteResponseNode.path("success").asBoolean());
                    deleteResponse.put("message", deleteResponseNode.path("message").asText());
                    deleteResponse.put("id", id);
                    
                    responseModel.setData(deleteResponse);
                    responseModel.setMessage("Role deleted successfully");
                    responseModel.setStatus(200);
                    responseModel.setError(null);
                } else {
                    responseModel.setData(graphqlResponse);
                    responseModel.setMessage("Error deleting role");
                    responseModel.setStatus(500);
                    responseModel.setError("Unable to delete role");
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