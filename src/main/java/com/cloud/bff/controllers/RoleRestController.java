package com.cloud.bff.controllers;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.RoleModel;
import com.cloud.bff.services.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/roles/rest")
public class RoleRestController {

    private final RoleService roleService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(RoleRestController.class);

    public RoleRestController(RoleService roleService, ObjectMapper objectMapper) {
        this.roleService = roleService;
        this.objectMapper = objectMapper;
    }

    // Single endpoint that handles all HTTP methods
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseModel handleRequest(HttpServletRequest request) {
        try {
            String method = request.getMethod();
            logger.info("Handling {} request for roles", method);
            
            switch (method) {
                case "GET":
                    return handleGet(request);
                case "POST":
                    return handlePost(request);
                case "PUT":
                    return handlePut(request);
                case "DELETE":
                    return handleDelete(request);
                default:
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(405);
                    errorResponse.setMessage("Method not allowed");
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
    
    private ResponseModel handleGet(HttpServletRequest request) {
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
    
    private ResponseModel handlePost(HttpServletRequest request) throws IOException {
        RoleModel role = objectMapper.readValue(request.getInputStream(), RoleModel.class);
        if (role.getTitle() == null || role.getTitle().isEmpty()) {
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(400);
            errorResponse.setMessage("Title is required");
            return errorResponse;
        }
        return roleService.createRole(role);
    }
    
    private ResponseModel handlePut(HttpServletRequest request) throws IOException {
        RoleModel role = objectMapper.readValue(request.getInputStream(), RoleModel.class);
        if (role.getId() == null) {
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(400);
            errorResponse.setMessage("Role ID is required for update");
            return errorResponse;
        }
        return roleService.updateRole(role);
    }
    
    private ResponseModel handleDelete(HttpServletRequest request) throws IOException {
        // Check if ID is provided as a path parameter
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/")) {
            try {
                Long id = Long.parseLong(pathInfo.substring(1));
                return roleService.deleteRole(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid role ID format");
                return errorResponse;
            }
        }
        
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
}