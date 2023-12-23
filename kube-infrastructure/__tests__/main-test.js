"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// Copyright (c) HashiCorp, Inc
// SPDX-License-Identifier: MPL-2.0
require("cdktf/lib/testing/adapters/jest"); // Load types for expect matchers
// import { Testing } from "cdktf";
describe("My CDKTF Application", () => {
    // The tests below are example tests, you can find more information at
    // https://cdk.tf/testing
    it.todo("should be tested");
    // // All Unit tests test the synthesised terraform code, it does not create real-world resources
    // describe("Unit testing using assertions", () => {
    //   it("should contain a resource", () => {
    //     // import { Image,Container } from "./.gen/providers/docker"
    //     expect(
    //       Testing.synthScope((scope) => {
    //         new MyApplicationsAbstraction(scope, "my-app", {});
    //       })
    //     ).toHaveResource(Container);
    //     expect(
    //       Testing.synthScope((scope) => {
    //         new MyApplicationsAbstraction(scope, "my-app", {});
    //       })
    //     ).toHaveResourceWithProperties(Image, { name: "ubuntu:latest" });
    //   });
    // });
    // describe("Unit testing using snapshots", () => {
    //   it("Tests the snapshot", () => {
    //     const app = Testing.app();
    //     const stack = new TerraformStack(app, "test");
    //     new TestProvider(stack, "provider", {
    //       accessKey: "1",
    //     });
    //     new TestResource(stack, "test", {
    //       name: "my-resource",
    //     });
    //     expect(Testing.synth(stack)).toMatchSnapshot();
    //   });
    //   it("Tests a combination of resources", () => {
    //     expect(
    //       Testing.synthScope((stack) => {
    //         new TestDataSource(stack, "test-data-source", {
    //           name: "foo",
    //         });
    //         new TestResource(stack, "test-resource", {
    //           name: "bar",
    //         });
    //       })
    //     ).toMatchInlineSnapshot();
    //   });
    // });
    // describe("Checking validity", () => {
    //   it("check if the produced terraform configuration is valid", () => {
    //     const app = Testing.app();
    //     const stack = new TerraformStack(app, "test");
    //     new TestDataSource(stack, "test-data-source", {
    //       name: "foo",
    //     });
    //     new TestResource(stack, "test-resource", {
    //       name: "bar",
    //     });
    //     expect(Testing.fullSynth(app)).toBeValidTerraform();
    //   });
    //   it("check if this can be planned", () => {
    //     const app = Testing.app();
    //     const stack = new TerraformStack(app, "test");
    //     new TestDataSource(stack, "test-data-source", {
    //       name: "foo",
    //     });
    //     new TestResource(stack, "test-resource", {
    //       name: "bar",
    //     });
    //     expect(Testing.fullSynth(app)).toPlanSuccessfully();
    //   });
    // });
});
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibWFpbi10ZXN0LmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsibWFpbi10ZXN0LnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7O0FBQUEsK0JBQStCO0FBQy9CLG1DQUFtQztBQUNuQywyQ0FBeUMsQ0FBQyxpQ0FBaUM7QUFDM0UsbUNBQW1DO0FBRW5DLFFBQVEsQ0FBQyxzQkFBc0IsRUFBRSxHQUFHLEVBQUU7SUFDbEMsc0VBQXNFO0lBQ3RFLHlCQUF5QjtJQUN6QixFQUFFLENBQUMsSUFBSSxDQUFDLGtCQUFrQixDQUFDLENBQUM7SUFFNUIsaUdBQWlHO0lBQ2pHLG9EQUFvRDtJQUNwRCw0Q0FBNEM7SUFDNUMsbUVBQW1FO0lBQ25FLGNBQWM7SUFDZCx3Q0FBd0M7SUFDeEMsOERBQThEO0lBQzlELFdBQVc7SUFDWCxtQ0FBbUM7SUFFbkMsY0FBYztJQUNkLHdDQUF3QztJQUN4Qyw4REFBOEQ7SUFDOUQsV0FBVztJQUNYLHdFQUF3RTtJQUN4RSxRQUFRO0lBQ1IsTUFBTTtJQUVOLG1EQUFtRDtJQUNuRCxxQ0FBcUM7SUFDckMsaUNBQWlDO0lBQ2pDLHFEQUFxRDtJQUVyRCw0Q0FBNEM7SUFDNUMsd0JBQXdCO0lBQ3hCLFVBQVU7SUFFVix3Q0FBd0M7SUFDeEMsNkJBQTZCO0lBQzdCLFVBQVU7SUFFVixzREFBc0Q7SUFDdEQsUUFBUTtJQUVSLG1EQUFtRDtJQUNuRCxjQUFjO0lBQ2Qsd0NBQXdDO0lBQ3hDLDBEQUEwRDtJQUMxRCx5QkFBeUI7SUFDekIsY0FBYztJQUVkLHFEQUFxRDtJQUNyRCx5QkFBeUI7SUFDekIsY0FBYztJQUNkLFdBQVc7SUFDWCxpQ0FBaUM7SUFDakMsUUFBUTtJQUNSLE1BQU07SUFFTix3Q0FBd0M7SUFDeEMseUVBQXlFO0lBQ3pFLGlDQUFpQztJQUNqQyxxREFBcUQ7SUFFckQsc0RBQXNEO0lBQ3RELHFCQUFxQjtJQUNyQixVQUFVO0lBRVYsaURBQWlEO0lBQ2pELHFCQUFxQjtJQUNyQixVQUFVO0lBQ1YsMkRBQTJEO0lBQzNELFFBQVE7SUFFUiwrQ0FBK0M7SUFDL0MsaUNBQWlDO0lBQ2pDLHFEQUFxRDtJQUVyRCxzREFBc0Q7SUFDdEQscUJBQXFCO0lBQ3JCLFVBQVU7SUFFVixpREFBaUQ7SUFDakQscUJBQXFCO0lBQ3JCLFVBQVU7SUFDViwyREFBMkQ7SUFDM0QsUUFBUTtJQUNSLE1BQU07QUFDVixDQUFDLENBQUMsQ0FBQyIsInNvdXJjZXNDb250ZW50IjpbIi8vIENvcHlyaWdodCAoYykgSGFzaGlDb3JwLCBJbmNcbi8vIFNQRFgtTGljZW5zZS1JZGVudGlmaWVyOiBNUEwtMi4wXG5pbXBvcnQgXCJjZGt0Zi9saWIvdGVzdGluZy9hZGFwdGVycy9qZXN0XCI7IC8vIExvYWQgdHlwZXMgZm9yIGV4cGVjdCBtYXRjaGVyc1xuLy8gaW1wb3J0IHsgVGVzdGluZyB9IGZyb20gXCJjZGt0ZlwiO1xuXG5kZXNjcmliZShcIk15IENES1RGIEFwcGxpY2F0aW9uXCIsICgpID0+IHtcbiAgICAvLyBUaGUgdGVzdHMgYmVsb3cgYXJlIGV4YW1wbGUgdGVzdHMsIHlvdSBjYW4gZmluZCBtb3JlIGluZm9ybWF0aW9uIGF0XG4gICAgLy8gaHR0cHM6Ly9jZGsudGYvdGVzdGluZ1xuICAgIGl0LnRvZG8oXCJzaG91bGQgYmUgdGVzdGVkXCIpO1xuXG4gICAgLy8gLy8gQWxsIFVuaXQgdGVzdHMgdGVzdCB0aGUgc3ludGhlc2lzZWQgdGVycmFmb3JtIGNvZGUsIGl0IGRvZXMgbm90IGNyZWF0ZSByZWFsLXdvcmxkIHJlc291cmNlc1xuICAgIC8vIGRlc2NyaWJlKFwiVW5pdCB0ZXN0aW5nIHVzaW5nIGFzc2VydGlvbnNcIiwgKCkgPT4ge1xuICAgIC8vICAgaXQoXCJzaG91bGQgY29udGFpbiBhIHJlc291cmNlXCIsICgpID0+IHtcbiAgICAvLyAgICAgLy8gaW1wb3J0IHsgSW1hZ2UsQ29udGFpbmVyIH0gZnJvbSBcIi4vLmdlbi9wcm92aWRlcnMvZG9ja2VyXCJcbiAgICAvLyAgICAgZXhwZWN0KFxuICAgIC8vICAgICAgIFRlc3Rpbmcuc3ludGhTY29wZSgoc2NvcGUpID0+IHtcbiAgICAvLyAgICAgICAgIG5ldyBNeUFwcGxpY2F0aW9uc0Fic3RyYWN0aW9uKHNjb3BlLCBcIm15LWFwcFwiLCB7fSk7XG4gICAgLy8gICAgICAgfSlcbiAgICAvLyAgICAgKS50b0hhdmVSZXNvdXJjZShDb250YWluZXIpO1xuXG4gICAgLy8gICAgIGV4cGVjdChcbiAgICAvLyAgICAgICBUZXN0aW5nLnN5bnRoU2NvcGUoKHNjb3BlKSA9PiB7XG4gICAgLy8gICAgICAgICBuZXcgTXlBcHBsaWNhdGlvbnNBYnN0cmFjdGlvbihzY29wZSwgXCJteS1hcHBcIiwge30pO1xuICAgIC8vICAgICAgIH0pXG4gICAgLy8gICAgICkudG9IYXZlUmVzb3VyY2VXaXRoUHJvcGVydGllcyhJbWFnZSwgeyBuYW1lOiBcInVidW50dTpsYXRlc3RcIiB9KTtcbiAgICAvLyAgIH0pO1xuICAgIC8vIH0pO1xuXG4gICAgLy8gZGVzY3JpYmUoXCJVbml0IHRlc3RpbmcgdXNpbmcgc25hcHNob3RzXCIsICgpID0+IHtcbiAgICAvLyAgIGl0KFwiVGVzdHMgdGhlIHNuYXBzaG90XCIsICgpID0+IHtcbiAgICAvLyAgICAgY29uc3QgYXBwID0gVGVzdGluZy5hcHAoKTtcbiAgICAvLyAgICAgY29uc3Qgc3RhY2sgPSBuZXcgVGVycmFmb3JtU3RhY2soYXBwLCBcInRlc3RcIik7XG5cbiAgICAvLyAgICAgbmV3IFRlc3RQcm92aWRlcihzdGFjaywgXCJwcm92aWRlclwiLCB7XG4gICAgLy8gICAgICAgYWNjZXNzS2V5OiBcIjFcIixcbiAgICAvLyAgICAgfSk7XG5cbiAgICAvLyAgICAgbmV3IFRlc3RSZXNvdXJjZShzdGFjaywgXCJ0ZXN0XCIsIHtcbiAgICAvLyAgICAgICBuYW1lOiBcIm15LXJlc291cmNlXCIsXG4gICAgLy8gICAgIH0pO1xuXG4gICAgLy8gICAgIGV4cGVjdChUZXN0aW5nLnN5bnRoKHN0YWNrKSkudG9NYXRjaFNuYXBzaG90KCk7XG4gICAgLy8gICB9KTtcblxuICAgIC8vICAgaXQoXCJUZXN0cyBhIGNvbWJpbmF0aW9uIG9mIHJlc291cmNlc1wiLCAoKSA9PiB7XG4gICAgLy8gICAgIGV4cGVjdChcbiAgICAvLyAgICAgICBUZXN0aW5nLnN5bnRoU2NvcGUoKHN0YWNrKSA9PiB7XG4gICAgLy8gICAgICAgICBuZXcgVGVzdERhdGFTb3VyY2Uoc3RhY2ssIFwidGVzdC1kYXRhLXNvdXJjZVwiLCB7XG4gICAgLy8gICAgICAgICAgIG5hbWU6IFwiZm9vXCIsXG4gICAgLy8gICAgICAgICB9KTtcblxuICAgIC8vICAgICAgICAgbmV3IFRlc3RSZXNvdXJjZShzdGFjaywgXCJ0ZXN0LXJlc291cmNlXCIsIHtcbiAgICAvLyAgICAgICAgICAgbmFtZTogXCJiYXJcIixcbiAgICAvLyAgICAgICAgIH0pO1xuICAgIC8vICAgICAgIH0pXG4gICAgLy8gICAgICkudG9NYXRjaElubGluZVNuYXBzaG90KCk7XG4gICAgLy8gICB9KTtcbiAgICAvLyB9KTtcblxuICAgIC8vIGRlc2NyaWJlKFwiQ2hlY2tpbmcgdmFsaWRpdHlcIiwgKCkgPT4ge1xuICAgIC8vICAgaXQoXCJjaGVjayBpZiB0aGUgcHJvZHVjZWQgdGVycmFmb3JtIGNvbmZpZ3VyYXRpb24gaXMgdmFsaWRcIiwgKCkgPT4ge1xuICAgIC8vICAgICBjb25zdCBhcHAgPSBUZXN0aW5nLmFwcCgpO1xuICAgIC8vICAgICBjb25zdCBzdGFjayA9IG5ldyBUZXJyYWZvcm1TdGFjayhhcHAsIFwidGVzdFwiKTtcblxuICAgIC8vICAgICBuZXcgVGVzdERhdGFTb3VyY2Uoc3RhY2ssIFwidGVzdC1kYXRhLXNvdXJjZVwiLCB7XG4gICAgLy8gICAgICAgbmFtZTogXCJmb29cIixcbiAgICAvLyAgICAgfSk7XG5cbiAgICAvLyAgICAgbmV3IFRlc3RSZXNvdXJjZShzdGFjaywgXCJ0ZXN0LXJlc291cmNlXCIsIHtcbiAgICAvLyAgICAgICBuYW1lOiBcImJhclwiLFxuICAgIC8vICAgICB9KTtcbiAgICAvLyAgICAgZXhwZWN0KFRlc3RpbmcuZnVsbFN5bnRoKGFwcCkpLnRvQmVWYWxpZFRlcnJhZm9ybSgpO1xuICAgIC8vICAgfSk7XG5cbiAgICAvLyAgIGl0KFwiY2hlY2sgaWYgdGhpcyBjYW4gYmUgcGxhbm5lZFwiLCAoKSA9PiB7XG4gICAgLy8gICAgIGNvbnN0IGFwcCA9IFRlc3RpbmcuYXBwKCk7XG4gICAgLy8gICAgIGNvbnN0IHN0YWNrID0gbmV3IFRlcnJhZm9ybVN0YWNrKGFwcCwgXCJ0ZXN0XCIpO1xuXG4gICAgLy8gICAgIG5ldyBUZXN0RGF0YVNvdXJjZShzdGFjaywgXCJ0ZXN0LWRhdGEtc291cmNlXCIsIHtcbiAgICAvLyAgICAgICBuYW1lOiBcImZvb1wiLFxuICAgIC8vICAgICB9KTtcblxuICAgIC8vICAgICBuZXcgVGVzdFJlc291cmNlKHN0YWNrLCBcInRlc3QtcmVzb3VyY2VcIiwge1xuICAgIC8vICAgICAgIG5hbWU6IFwiYmFyXCIsXG4gICAgLy8gICAgIH0pO1xuICAgIC8vICAgICBleHBlY3QoVGVzdGluZy5mdWxsU3ludGgoYXBwKSkudG9QbGFuU3VjY2Vzc2Z1bGx5KCk7XG4gICAgLy8gICB9KTtcbiAgICAvLyB9KTtcbn0pO1xuIl19