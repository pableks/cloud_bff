package com.cloud.bff.services;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.UserModel;

public interface UserService {

    public ResponseModel getUsers();

    public ResponseModel addUser(UserModel user);

    public ResponseModel updateUser(UserModel user);

    public ResponseModel deleteUser(Long id);
}
