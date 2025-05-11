package com.cloud.bff.services.serviceImpl;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UserServiceImpl implements UserService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public UserServiceImpl(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://funcionduoc.azurewebsites.net/api").build();
        this.objectMapper = objectMapper;
    }
    @Override
    public ResponseModel getUsers() {
        ResponseModel responseModel = new ResponseModel();

        try {
            responseModel.setData(
                    webClient.get()
                            .uri("/ClientRest?code=kqD4zMtf_1q5A4WUO2izA-lSfWDa98Fmpl5mPxgL8LhUAzFu6V7PDw=="
                                    )
                            .retrieve()

                            .bodyToFlux(UserModel.class)
                            .collectList()
                            .block()
            );
            responseModel.setMessage("Success");
            responseModel.setStatus(200);
            responseModel.setError(null);

            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }

    }

    @Override
    public ResponseModel addUser(UserModel user) {
        ResponseModel responseModel = new ResponseModel();


        try {

            ObjectNode json = objectMapper.valueToTree(user);

            json.remove("id");

            System.out.println(json.toString());

            responseModel.setData(
                    webClient.post()
                            .uri("/ClientRest?code=kqD4zMtf_1q5A4WUO2izA-lSfWDa98Fmpl5mPxgL8LhUAzFu6V7PDw==")
                            .header("Content-Type", "application/json")
                            .bodyValue(json)
                            .exchangeToMono(response -> {
                                System.out.println(response.statusCode());
                                return response.bodyToMono(String.class);
                            })
                            .block()
            );

            String response = responseModel.getData().toString();

            if (response.contains("ORA-00001")) {
                responseModel.setMessage("Failed");
                responseModel.setData(null);
                responseModel.setStatus(500);
                responseModel.setError(response);
            } else {
                responseModel.setMessage("Success");
                responseModel.setStatus(200);
                responseModel.setError(null);
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
    public ResponseModel updateUser(UserModel user) {
        ResponseModel responseModel = new ResponseModel();

        try {
            responseModel.setData(
                    webClient.put()
                            .uri("/ClientRest?code=kqD4zMtf_1q5A4WUO2izA-lSfWDa98Fmpl5mPxgL8LhUAzFu6V7PDw==")
                            .header("Content-Type", "application/json")
                            .bodyValue(user)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block()
            );
            responseModel.setMessage("Success");
            responseModel.setStatus(200);
            responseModel.setError(null);

            return responseModel;

        } catch (Exception e) {
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel deleteUser(Long id) {
        ResponseModel responseModel = new ResponseModel();

        try {
            System.out.println(id);
            responseModel.setData(
              webClient.method(HttpMethod.DELETE)
                      .uri("/ClientRest?code=kqD4zMtf_1q5A4WUO2izA-lSfWDa98Fmpl5mPxgL8LhUAzFu6V7PDw==")
                      .bodyValue(id)
                      .retrieve()
                      .bodyToMono(String.class)
                      .block()
            );

            responseModel.setMessage("Success");
            responseModel.setStatus(200);
            responseModel.setError(null);

            String resp = responseModel.getData().toString();

            if (resp.contains("No se encontró un usuario con ID")) {
                responseModel.setMessage("Failed");
                responseModel.setData(null);
                responseModel.setStatus(500);
                responseModel.setError(resp);
            } else {
                responseModel.setMessage("Success");
                responseModel.setStatus(200);
                responseModel.setError(null);
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
