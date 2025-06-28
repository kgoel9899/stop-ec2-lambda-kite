package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

public class StopEC2Instance implements RequestHandler<Object, String> {

    private static final String TAG_KEY = "CreatedFor";
    private static final String TAG_VALUE = "Kite";

    @Override
    public String handleRequest(Object input, Context context) {
        try (Ec2Client ec2 = Ec2Client.create()) {

            // Step 1: Filter instances by tag
            Filter tagFilter = Filter.builder()
                    .name("tag:" + TAG_KEY)
                    .values(TAG_VALUE)
                    .build();

            DescribeInstancesResponse describeResponse = ec2.describeInstances(
                    DescribeInstancesRequest.builder()
                            .filters(tagFilter)
                            .build());

            List<String> instanceIdsToTerminate = new ArrayList<>();

            for (Reservation reservation : describeResponse.reservations()) {
                for (Instance instance : reservation.instances()) {
                    if (instance.state().name() != InstanceStateName.TERMINATED) {
                        instanceIdsToTerminate.add(instance.instanceId());
                    }
                }
            }

            if (instanceIdsToTerminate.isEmpty()) {
                return "No instances found with tag " + TAG_KEY + "=" + TAG_VALUE;
            }

            // Step 2: Terminate them
            ec2.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(instanceIdsToTerminate)
                    .build());

            return "Terminated instances: " + instanceIdsToTerminate;

        } catch (Exception e) {
            return "Failed to terminate instances: " + e.getMessage();
        }
    }
}
