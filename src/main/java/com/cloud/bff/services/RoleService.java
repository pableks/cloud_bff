package com.cloud.bff.services;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.RoleModel;

public interface RoleService {

    public ResponseModel getRoles();
    
    public ResponseModel getRoleById(Long id);
    
    public ResponseModel createRole(RoleModel role);
    
    public ResponseModel updateRole(RoleModel role);
    
    public ResponseModel deleteRole(Long id);
} 