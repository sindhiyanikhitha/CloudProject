import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.util.UUID;

public class EmailLambda implements RequestHandler<SNSEvent, Object> {
    static DynamoDB dynamoDB;

    public Object handleRequest(SNSEvent request, Context context) {

        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());

        String domain = System.getenv("domain");

        final String FROM = "no-reply@" + domain;
        String billPayload = request.getRecords().get(0).getSNS().getMessage();
        JsonObject jsonObject = new JsonParser().parse(billPayload).getAsJsonObject();
        context.getLogger().log("Payload check");
        String email = "";
        String TO = jsonObject.get("email").getAsString();
        JsonArray listBillId =jsonObject.getAsJsonArray("billList");
        context.getLogger().log("List of dude bills");
        for(int i = 0; i<listBillId.size();i++){
            String temp = listBillId.get(i).toString();
            email += "<p><a href='#'>https://" + domain + "/v1/bill/"+ temp +"</a></p><br>";
            email =  email.replaceAll("\"","");
        }
        context.getLogger().log(email);
        try {

            context.getLogger().log("connect to dynamodb");
            init();
            Table table = dynamoDB.getTable("csye6225");
            long unixTime = Instant.now().getEpochSecond()+60*60;
            long now = Instant.now().getEpochSecond();
            if (table == null) {
                context.getLogger().log("table doesnt exist");
            } else {
                context.getLogger().log("request object:"+request.getRecords().get(0).getSNS().getMessage());
                Item item = table.getItem("email",TO);
                context.getLogger().log("Item Object"+item);
                if (item == null || (item!=null && Long.parseLong(item.get("ttlInMin").toString())< now)) {
                    String token = UUID.randomUUID().toString();
                    Item itemPut = new Item()
                            .withPrimaryKey("email", TO).withNumber("ttlInMin", unixTime);

                    context.getLogger().log("AWSRequest ID:" + context.getAwsRequestId());
                    table.putItem(itemPut);
                    context.getLogger().log("AWSMessage ID:" + request.getRecords().get(0).getSNS().getMessageId());
                    AmazonSimpleEmailService client =
                            AmazonSimpleEmailServiceClientBuilder.standard()
                                    .withRegion(Regions.US_EAST_1).build();
                    SendEmailRequest req = new SendEmailRequest()
                            .withDestination(
                                    new Destination()
                                            .withToAddresses(TO))
                            .withMessage(
                                    new Message()
                                            .withBody(
                                                    new Body()
                                                            .withHtml(
                                                                    new Content()
                                                                            .withCharset(
                                                                                    "UTF-8")
                                                                            .withData(
                                                                                    "Please find your list of due bills below <br>" +
                                                                                            email))
                                            )
                                            .withSubject(
                                                    new Content().withCharset("UTF-8")
                                                            .withData("Your request for list of Due Bills ")))
                            .withSource(FROM);
                    SendEmailResult response = client.sendEmail(req);
                    System.out.println("Email sending success!");
                } else {
                    context.getLogger().log(item.toJSON() + "The email is sent already!");
                }
            }
        } catch (Exception ex) {
            context.getLogger().log("Email sending failed because of : "
                    + ex.getMessage());
        }

        return null;
    }

    private static void init() throws Exception {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        dynamoDB = new DynamoDB(client);
    }

}