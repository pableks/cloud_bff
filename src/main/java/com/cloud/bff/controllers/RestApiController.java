package com.cloud.bff.controllers;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.RoleModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.RoleService;
import com.cloud.bff.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/rest")
public class RestApiController {

    private final RoleService roleService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(RestApiController.class);

    public RestApiController(RoleService roleService, UserService userService, ObjectMapper objectMapper) {
        this.roleService = roleService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseModel handleRequest(HttpServletRequest request) {
        try {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String resourceType = getResourceType(path, request.getParameter("resource"));
            
            logger.info("Handling {} request for {} at {}", method, resourceType, path);
            
            if ("roles".equals(resourceType)) {
                return handleRoleRequest(method, request);
            } else if ("users".equals(resourceType)) {
                return handleUserRequest(method, request);
            } else {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid or missing resource type. Use 'resource=roles' or 'resource=users' parameter.");
                return errorResponse;
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage(), e);
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(500);
            errorResponse.setMessage("Internal server error");
            errorResponse.setError(e.getMessage());
            return errorResponse;
        }
    }
    
    private String getResourceType(String path, String resourceParam) {
        // First try the resource query parameter
        if (resourceParam != null && !resourceParam.isEmpty()) {
            return resourceParam.toLowerCase();
        }
        
        // Then try to extract from path
        if (path != null) {
            path = path.toLowerCase();
            if (path.contains("/roles")) {
                return "roles";
            } else if (path.contains("/users")) {
                return "users";
            }
        }
        
        // Default to a parameter
        return resourceParam;
    }
    
    // Role request handlers
    private ResponseModel handleRoleRequest(String method, HttpServletRequest request) throws IOException {
        switch (method) {
            case "GET":
                return handleRoleGet(request);
            case "POST":
                return handleRolePost(request);
            case "PUT":
                return handleRolePut(request);
            case "DELETE":
                return handleRoleDelete(request);
            default:
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(405);
                errorResponse.setMessage("Method not allowed");
                return errorResponse;
        }
    }
    
    private ResponseModel handleRoleGet(HttpServletRequest request) {
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                Long id = Long.parseLong(idParam);
                return roleService.getRoleById(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid role ID format");
                return errorResponse;
            }
        } else {
            return roleService.getRoles();
        }
    }
    
    private ResponseModel handleRolePost(HttpServletRequest request) throws IOException {
        RoleModel role = objectMapper.readValue(request.getInputStream(), RoleModel.class);
        if (role.getTitle() == null || role.getTitle().isEmpty()) {
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(400);
            errorResponse.setMessage("Title is required");
            return errorResponse;
        }
        return roleService.createRole(role);
    }
    
    private ResponseModel handleRolePut(HttpServletRequest request) throws IOException {
        RoleModel role = objectMapper.readValue(request.getInputStream(), RoleModel.class);
        if (role.getId() == null) {
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(400);
            errorResponse.setMessage("Role ID is required for update");
            return errorResponse;
        }
        return roleService.updateRole(role);
    }
    
    private ResponseModel handleRoleDelete(HttpServletRequest request) throws IOException {
        // Check if ID is provided as a query parameter
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                Long id = Long.parseLong(idParam);
                return roleService.deleteRole(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid role ID format");
                return errorResponse;
            }
        }
        
        // Check if ID is provided in the request body
        try {
            RoleModel role = objectMapper.readValue(request.getInputStream(), RoleModel.class);
            if (role.getId() != null) {
                return roleService.deleteRole(role.getId());
            }
        } catch (Exception e) {
            // Ignore errors reading the body, continue checking other methods
        }
        
        ResponseModel errorResponse = new ResponseModel();
        errorResponse.setStatus(400);
        errorResponse.setMessage("Role ID is required for DELETE request");
        return errorResponse;
    }
    
    // User request handlers
    private ResponseModel handleUserRequest(String method, HttpServletRequest request) throws IOException {
        switch (method) {
            case "GET":
                return handleUserGet(request);
            case "POST":
                return handleUserPost(request);
            case "PUT":
                return handleUserPut(request);
            case "DELETE":
                return handleUserDelete(request);
            default:
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(405);
                errorResponse.setMessage("Method not allowed");
                return errorResponse;
        }
    }
    
    private ResponseModel handleUserGet(HttpServletRequest request) {
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                Long id = Long.parseLong(idParam);
                return userService.getUserById(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid user ID format");
                return errorResponse;
            }
        } else {
            return userService.getUsers();
        }
    }
    
    private ResponseModel handleUserPost(HttpServletRequest request) throws IOException {
        UserModel user = objectMapper.readValue(request.getInputStream(), UserModel.class);
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(400);
            errorResponse.setMessage("Email is required");
            return errorResponse;
        }
        return userService.addUser(user);
    }
    
    private ResponseModel handleUserPut(HttpServletRequest request) throws IOException {
        UserModel user = objectMapper.readValue(request.getInputStream(), UserModel.class);
        if (user.getId() == null) {
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(400);
            errorResponse.setMessage("User ID is required for update");
            return errorResponse;
        }
        return userService.updateUser(user);
    }
    
    private ResponseModel handleUserDelete(HttpServletRequest request) throws IOException {
        // Check if ID is provided as a query parameter
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                Long id = Long.parseLong(idParam);
                return userService.deleteUser(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid user ID format");
                return errorResponse;
            }
        }
        
        // Check if ID is provided in the request body
        try {
            UserModel user = objectMapper.readValue(request.getInputStream(), UserModel.class);
            if (user.getId() != null) {
                return userService.deleteUser(user.getId());
            }
        } catch (Exception e) {
            // Ignore errors reading the body, continue checking other methods
        }
        
        ResponseModel errorResponse = new ResponseModel();
        errorResponse.setStatus(400);
        errorResponse.setMessage("User ID is required for DELETE request");
        return errorResponse;
    }
}