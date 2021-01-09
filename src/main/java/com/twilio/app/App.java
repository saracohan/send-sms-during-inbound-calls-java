package com.twilio.app;

import static spark.Spark.*;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.VoiceResponse;
import com.twilio.exception.ApiException;
import com.twilio.type.PhoneNumber;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Call;


public class App {
    public static void main(String[] args) {
        port(8080);
        post("/answer", (req, res) -> {
            // get the urlencoded form parameters
			Map<String, String> bodyMap = bodyToMap(req.body());
			String decodedCallerNumber  = URLDecoder.decode(bodyMap.get("UserIdentifier"), "UTF-8");

            sendSms(decodedCallerNumber);
            VoiceResponse twiml = new VoiceResponse.Builder()
                .say(new Say.Builder("Thanks for calling! We just sent you a text with a clue.")
                      .voice(Say.Voice.ALICE)
                      .build())
                .build();

			// Render TwiML as XML
			res.type("text/xml");

			String xml = twiml.toXml();

			JSONArray array = new JSONArray();
			JSONObject sayJson = new JSONObject().put("say", "Thanks for calling!  We just sent you a "
					+ "text with a "
					+ "clue.");
			JSONObject redirectJson = new JSONObject().put("redirect", "task://goodbye");
			array.put(sayJson)
					.put(redirectJson);
			String jsonString = new JSONObject()
					.put("actions", array)
					.toString();


			return jsonString;
        });

    }

    public static void sendSms(String toNumber) {
		String fromNumber = null;
		String accountSid = System.getenv("ACCOUNT_SID");
        String authToken = System.getenv("AUTH_TOKEN");
		Twilio.init(accountSid, authToken);

		ResourceSet<Call> calls = Call.reader().limit(1).read();
		for(Call record : calls) {
			//System.out.println(record.getSid());
			fromNumber = record.getTo();
		}

		try {
            Message
                .creator(new PhoneNumber(toNumber),
                         new PhoneNumber(fromNumber),
                        "Please use our Closing Costs Calculator here: http://closings.lexpand.ca. "
								+ "Select your transaction type and enter the required information. "
								+ "Enter your email address at the last step and the system will automatically email "
								+ "a fairly detailed funds summary to you. If you wish to retain us for your closing, "
								+ "click on the Hire Us Now button in the email and submit the form. "
								+ "We will then open your file and send you the initial correspondence with more information "
								+ "about the rest of the process.")
                .create();
        } catch (ApiException e) {
            if (e.getCode() == 21614) {
                System.out.println("Uh oh, looks like this caller can't receive SMS messages.");
            }
        }
    }

	public static Map<String, String> bodyToMap(String bodyStr) {
		Map<String, String> body = new HashMap<>();
		String[] values = bodyStr.split("&");
		for (String value : values) {
			String[] pair = value.split("=");
			if (pair.length == 2) {
				body.put(pair[0], pair[1]);
			}
		}
		return body;
	}
}
