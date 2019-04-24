import java.net.MalformedURLException; 
import java.net.URL; 
import java.net.URLEncoder; 
import java.security.cert.Certificate; 
import java.io.*; 
 
import javax.net.ssl.HttpsURLConnection; 
import javax.net.ssl.SSLPeerUnverifiedException;
import java.util.Base64; 
import java.util.Map; 
import java.util.Set; 
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Sched {
	
	
	// Function to encode URL to schedule a job
	private static String encodeURL(String vScheduleTemplate, String vTemplateName, String vTemplateText, String vExecuteUser) {
		try {
			
			// Encode the URL
			vScheduleTemplate = vScheduleTemplate.replace(vTemplateName, URLEncoder.encode(vTemplateName, "UTF-8"));
			vScheduleTemplate = vScheduleTemplate.replace("TestingJob", URLEncoder.encode(vTemplateText, "UTF-8"));
			vScheduleTemplate = vScheduleTemplate.replace(vExecuteUser, URLEncoder.encode(vExecuteUser, "UTF-8"));
		
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
		}
		
		return vScheduleTemplate;
	}
	
	// Function to init get request to get templates
	private static HttpsURLConnection createGetTemplateRequest(String vUserPassword, String vBaseURL, String vGetTemplates) throws IOException {
		
		URL oURL;
		oURL = new URL(vBaseURL + vGetTemplates);
		
		// Open connection
		javax.net.ssl.HttpsURLConnection oConnection = (HttpsURLConnection) oURL.openConnection();
				
		// Print URL opened
		System.out.println(oURL.toString());
				
		// Initialize to do a GET request
		oConnection.setRequestMethod("GET");
		oConnection.setDoOutput(true);
				
		// Encode and print password
		String vUserPasswordEncoded = Base64.getEncoder().encodeToString(vUserPassword.getBytes());
		System.out.println("base64 = " + vUserPasswordEncoded);
				
				
		// Init request properties to send in request
		oConnection.setRequestProperty("Authorization", "Basic " + vUserPasswordEncoded);
		oConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		oConnection.setRequestProperty("Content-Type", "application/atom+xml");
		oConnection.setRequestProperty("DataServiceVersion", "2.0");
		oConnection.setRequestProperty("X-CSRF-Token", "Fetch");
				
		System.out.println("HTTPS GET request...");
		
		System.out.println("Response getTemplates: " + oConnection.getResponseCode() + ", " + oConnection.getResponseMessage());
		
		return oConnection;
	}
	
	// Function to read get template response
	private static String readTemplateResponse(HttpsURLConnection oConnection) throws IOException {
		
		BufferedReader oBufferedReader;
		
		// Good response - get input stream
		if (oConnection.getResponseCode() == 200) {
			
			oBufferedReader = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
			
		// Bad response - get error stream
		} else {
			
			oBufferedReader = new BufferedReader(new InputStreamReader(oConnection.getErrorStream()));
		}
	
	
		String vLine = null;
		String vTemplateList = null;
		
		// Read each line 
		do {
			
			vLine = oBufferedReader.readLine();
			
			// If line is there
			if (vLine != null) {
				
				// Remove spaces...I think?
				vLine = vLine.replaceAll("[^\\x20-\\xFF]", "");
				
				// If no templates assign the line to the template list
				if (vTemplateList == null)
					
					vTemplateList = vLine;
				
				// If entries in template list, build list
				else
					
					vTemplateList = vTemplateList + vLine;
				
				System.out.println(vLine);
			}
		} while (vLine != null);
		
		System.out.println();
		
		return vTemplateList;
	}
	
	// Function to print get template response
	private static String[] printTemplateResponse(HttpsURLConnection oConnection, String vTemplateList) throws IOException {
		
		// Good response
		if (oConnection.getResponseCode() == 200) {
			
			System.out.println("Possible templates to schedule a job");
			
			int vPos = 0;
			
			do {
				
				// Get index of beginning of template name field
				int vTemplateNamePos = vTemplateList.indexOf("<d:JobTemplateName>", vPos);
				
				// Get index of beginning of job template text field
				int vTemplateTextPos = vTemplateList.indexOf("<d:JobTemplateText>", vPos);
				
				// Check if regex found in string
				if ((vTemplateNamePos >= 0) && (vTemplateTextPos >= 0)) {
					
					// Add offset to get content
					vTemplateNamePos += 19;
					vTemplateTextPos += 19;
					
					// Find index of end of text to be extracted
					int vTemplateNamePosEnd = vTemplateList.indexOf("<", vTemplateNamePos);
					int vTemplateTextPosEnd = vTemplateList.indexOf("<", vTemplateTextPos);
					
					// Get the actual text between beginning/ending indexes
					String vTemplateListName = vTemplateList.substring(vTemplateNamePos, vTemplateNamePosEnd);
					String vTemplateListText = vTemplateList.substring(vTemplateTextPos, vTemplateTextPosEnd);
					
					// Print template info
					System.out.println("Template " + vTemplateListName + ": " + vTemplateListText);
				}
				
				// Get the larger of the two between name, text indexes
				vPos = Math.max(vTemplateNamePos, vTemplateTextPos) + 1;
			
			// Stay in loop until no more template names to be found
			} while (vTemplateList.indexOf("<d:JobTemplateName>", vPos) >= 0);
		}
		
		
		String vCookie = "";
		
		// Output parameters to the console
		System.out.println("Response Headers:");
		
		//  Create a map.. Header : Header Values []
		Map<String, List<String>> oHeader = oConnection.getHeaderFields();
		Set<Map.Entry<String, List<String>>> oHeaderEntrySet = oHeader.entrySet();
		
		// Print each value in map
		for (Map.Entry<String, List<String>> oHeaderEntry : oHeaderEntrySet) {
			
			String vHeaderName = oHeaderEntry.getKey();
			System.out.print(vHeaderName + " : ");
			
			List<String> oHeaderValues = oHeaderEntry.getValue();
			

			for (String vHeaderValue : oHeaderValues) {
				
				if (vHeaderValue != null)
					
					vHeaderValue = vHeaderValue.replaceAll("[^\\x20-\\xFF]", "");
				
				System.out.println(vHeaderValue);
			}
			
			// Check if parameter is the cookie to save it
			if ((vHeaderName != null) && (vHeaderName.equalsIgnoreCase("Set-Cookie"))) {
				for (String vHeaderValue : oHeaderValues) {
					vCookie = vCookie + "; " + vHeaderValue;
				}
			}
			System.out.println();
		}
		
		System.out.println("Set-Cookie: " + vCookie);
		
		// Assign CSRF Token
		String vCSRFToken = oConnection.getHeaderField("x-csrf-token");
		System.out.println("CSRF Token: " + vCSRFToken);
		
		String[] cookie_CSRF = {vCookie, vCSRFToken};
		return cookie_CSRF;
	}
	
	// Function to init post request to schedule job
	private static HttpsURLConnection createSchedulePostRequest(String vBaseURL, String vScheduleTemplate, String vCookie, String vCSRFToken) throws IOException {
		
		// Build URL
		URL oURL;
		oURL = new URL(vBaseURL + vScheduleTemplate);
		System.out.println(oURL.toString());
		
		// Open a new connection
		javax.net.ssl.HttpsURLConnection oConnectionPOST = (HttpsURLConnection) oURL.openConnection();
		
		// Set parameters for POST request
		oConnectionPOST.setRequestMethod("POST");
		oConnectionPOST.setDoOutput(true);
		oConnectionPOST.setRequestProperty("Cookie", vCookie);
		oConnectionPOST.setRequestProperty("X-CSRF-Token", vCSRFToken);
		
		System.out.println("HTTPS POST request...");
		
		// Print response
		System.out.println("Response Job Schedule: " + oConnectionPOST.getResponseCode() + ", " + oConnectionPOST.getResponseMessage());
		
		return oConnectionPOST;
		
	}
	
	// Function to print response from post schedule request
	private static String printSchedulePostResponse(HttpsURLConnection oConnectionPOST) throws IOException {
		
		BufferedReader oBufferedReader;
		
		// Good response - get input stream
		if (oConnectionPOST.getResponseCode() == 200) {
			
			oBufferedReader = new BufferedReader(new InputStreamReader(oConnectionPOST.getInputStream()));
		
		// Bad response - get error stream
		} else {
			
			oBufferedReader = new BufferedReader(new InputStreamReader(oConnectionPOST.getErrorStream()));
		}
	
		String vResponse = null;
		String vLine = null;
		
		// Read response
		do {
			
			vLine = oBufferedReader.readLine();
			
			if (vLine != null) {
				
				vLine = vLine.replaceAll("[^\\x20-\\xFF]", "");
				
				if (vResponse == null)
					
					vResponse = vLine;
				
				// Build response text string
				else 
					
					vResponse = vResponse + vLine;
				
				System.out.println(vLine);
			}
		
		} while (vLine != null);
		
		return vResponse;
	}
	
	// Function to get the job name and run count from post request response
	private static String[] getJobName_RunCount(HttpsURLConnection oConnectionPOST, String vResponse) throws IOException {
		
		String vJobName = null;
		String vJobRunCount = null;
		
		// Good response - get job name and job run count name
		if (oConnectionPOST.getResponseCode() == 200) {
			
			// Get starting indexes
			int vJobNamePos = vResponse.indexOf("<d:JobName>");
			int vJobRunCountPos = vResponse.indexOf("<d:JobRunCount>");
			
			// If found in regex - get text between indexes
			if ((vJobNamePos >= 0) && (vJobRunCountPos >= 0)) {
				
				// Add offset to index
				vJobNamePos += 11;
				vJobRunCountPos += 15;
				
				// Get text between the indexes
				vJobName = vResponse.substring(vJobNamePos, vJobNamePos + 32);
				vJobRunCount = vResponse.substring(vJobRunCountPos, vJobRunCountPos + 8);
				
				System.out.println("Scheduled JobName '" + vJobName + "', JobRunCount '" + vJobRunCount + "'");
			}
		
		// Bad response - display error message
		} else {
			
			int vPos = 0;
			
			do {
				
				// Get index of error message
				int vCodePos = vResponse.indexOf("<code>", vPos);
				int vMessagePos = vResponse.indexOf("<message", vPos);
				
				// String is found in regex - get text between indexes
				if ((vCodePos >= 0) && (vMessagePos >= 0)) {
					
					// Increment by offset
					vCodePos += 6;
					vMessagePos = vResponse.indexOf(">", vMessagePos + 8) + 1;
					
					// Get ending index
					int vCodePosEnd = vResponse.indexOf("<", vCodePos);
					int vMessagePosEnd = vResponse.indexOf("<", vMessagePos);
					
					// Extract error message between indexes
					if ((vCodePosEnd >= 0) && (vCodePos < vCodePosEnd)) {
						
						String vErrCode = vResponse.substring(vCodePos, vCodePosEnd);
						String vErrMessage = vResponse.substring(vMessagePos, vMessagePosEnd);
						
						System.out.println("Error " + vErrCode + " - " + vErrMessage);
					}
				}
				
				// Get max between message/code index position
				vPos = Math.max(vMessagePos, vCodePos);
			
			} while(vResponse.indexOf("<code>", vPos) > 0);
		}
		
		String[] jobName_RunCount = {vJobName, vJobRunCount};
		
		return jobName_RunCount;
	}
	
	// Function init get request to get the status of the scheduled job
	private static HttpsURLConnection createGetStatusRequest(String vJobName, String vJobRunCount, String vGetStatus, String vTemplateName, String vBaseURL, String vCookie, String vCSRFToken) throws IOException {
		
		// Encode jobname and jobruncount
		if ((vJobName != null) && (vJobRunCount != null)) {
			
			vGetStatus += vJobName + "'&JobRunCount='" + vJobRunCount + "'";
			
			try {
				
				vGetStatus = vGetStatus.replace(vTemplateName, URLEncoder.encode(vJobName, "UTF-8"));
				vGetStatus = vGetStatus.replace(vJobRunCount, URLEncoder.encode(vJobRunCount, "UTF-8"));
				
			} catch (UnsupportedEncodingException e) {
				
				e.printStackTrace();
			}
		}
			
		// Build URL to use to job status
		URL oURL;
		oURL = new URL(vBaseURL + vGetStatus);
		
		// Open a new connection
		javax.net.ssl.HttpsURLConnection oConnectionStatus = (HttpsURLConnection) oURL.openConnection();
		System.out.println(oURL.toString());
		
		// Set GET request parameters
		oConnectionStatus.setRequestMethod("GET");
		oConnectionStatus.setDoOutput(true);
		oConnectionStatus.setRequestProperty("Cookie", vCookie);
		oConnectionStatus.setRequestProperty("X-CSRF-Token", vCSRFToken);
		
		return oConnectionStatus;
				
	}
	
	// Function to read the get status response
	private static String readGetStatusResponse(HttpsURLConnection oConnectionStatus, String vTemplateList) throws IOException {
		
		BufferedReader oBufferedReader;
		
		// Output response
		System.out.println("Response GetStatus: " + oConnectionStatus.getResponseCode() + ", " + oConnectionStatus.getResponseMessage());
		
		// Good response - get input stream
		if (oConnectionStatus.getResponseCode() == 200) {
			
			oBufferedReader = new BufferedReader(new InputStreamReader(oConnectionStatus.getInputStream()));
			
		// Bad response - get error stream
		} else {
			
			oBufferedReader = new BufferedReader(new InputStreamReader(oConnectionStatus.getErrorStream()));
		}
		
		// Output response
		String vLine = null;
		String vJobStatus = null;
		
		// Read response
		do {
			
			vLine = oBufferedReader.readLine();
			
			if(vLine != null) {
				
				vLine = vLine.replaceAll("[^\\x20-\\xFF]", "");
				
				if (vJobStatus == null)
					
					vJobStatus = vLine;
				
				// Build job status message
				else
					
					vJobStatus = vTemplateList + vLine;
				
				System.out.println(vLine);
			}
		
		} while (vLine != null);
		
		System.out.println();
		
		return vJobStatus;
	}
	
	// Function to print the get status response
	private static char printGetStatusResponse(HttpsURLConnection oConnectionStatus, String vJobStatus) throws IOException {
		
		// Good connection - get job status from response
		if (oConnectionStatus.getResponseCode() == 200) {
			
			// Get beginning index
			int vJobStatusPos = vJobStatus.indexOf("<d:JobStatus>");
			
			// Found - increment index
			if (vJobStatusPos >= 0) {
				
				vJobStatusPos += 13;
				
				// Get ending index
				int vJobStatusPosEnd = vJobStatus.indexOf("<", vJobStatusPos);
				
				// Get the status code between the start/end indexes
				String vJobStatusCode = vJobStatus.substring(vJobStatusPos, vJobStatusPosEnd);
				String vJobStatusText;
				
				// Get first character of status code
				char vJobStatusCodeC = vJobStatusCode.charAt(0);
				
				// Determine status code meaning
				switch (vJobStatusCodeC) {
				
					case 'F': vJobStatusText = "Finished"; break;
					case 'R': vJobStatusText = "In Process"; break;
					case 'Y': vJobStatusText = "Ready"; break;
					case 'S': vJobStatusText = "Scheduled"; break;
					case 'A': vJobStatusText = "Failed"; break;
					case 'C': vJobStatusText = "Cancelled"; break;
					default: vJobStatusText = "unknown";
				}
				
				System.out.println("Job Status is '" + vJobStatusCode + "': " + vJobStatusText);
				return vJobStatusCodeC;
				
			} else {
				
				return 'A';
			}
			
		} else {
			
			return 'A';
		}
	}
		
	
	public static void main(String[] args) {

		System.out.println("Starting..."); 
		
		// Job/Template Info
		String vBaseURL      = "https://pt6-001-api.wdf.sap.corp/sap/opu/odata/sap/BC_EXT_APPJOB_MANAGEMENT;v=0002";   
		String vUserPassword = "ODATA_USER_TEST:Welcome1!";   
		String vTemplateName = "ZESFAPPKASAPORGUBONUIUOSE4E";   
		String vTemplateText = "Scheduled via Java";   
		String vExecuteUser  = "CB8980002901";
		
		// URLs to use to call API
		String vGetTemplates     = "/JobTemplateSet?";   
		String vScheduleTemplate = "/JobSchedule?JobTemplateName='" + vTemplateName + "'&JobText='TestingJob'&JobUser='" + vExecuteUser + "'";   
		String vGetStatus        = "/JobStatusGet?JobName='";
		
		try {
			
			// Create URL to schedule job
			vScheduleTemplate = encodeURL(vScheduleTemplate, vTemplateName, vTemplateText, vExecuteUser);
			
			// Init request to get templates
			HttpsURLConnection oConnection = createGetTemplateRequest(vUserPassword, vBaseURL, vGetTemplates);
			
			// Read the template get response
			String vTemplateList = readTemplateResponse(oConnection);
			
			// Print template response and save cookie and CSRF Token
			String[] cookie_CSRF = printTemplateResponse(oConnection, vTemplateList);
			String vCookie = cookie_CSRF[0];
			String vCSRFToken = cookie_CSRF[1];
			
			// Init POST request to schedule job
			HttpsURLConnection oConnectionPOST = createSchedulePostRequest(vBaseURL, vScheduleTemplate, vCookie, vCSRFToken);
		
			// Read the POST request response to schedule job
			String vResponse = printSchedulePostResponse(oConnectionPOST);
			
			// Get job name and job run count
			String[] jobName_runCount = getJobName_RunCount(oConnectionPOST, vResponse);
			String vJobName = jobName_runCount[0];
			String vJobRunCount = jobName_runCount[1];
			
			char vJobState;
			
			// While job is not finished, cancelled or scheduled check on the status
			do {
				// Init GET request to check status of job
				HttpsURLConnection oConnectionStatus = createGetStatusRequest(vJobName, vJobRunCount, vGetStatus, vTemplateName, vBaseURL, vCookie, vCSRFToken);
				
				// Read GET request to check status of job
				String vJobStatus = readGetStatusResponse(oConnectionStatus, vTemplateList);
				
				// Output get status response
				vJobState = printGetStatusResponse(oConnectionStatus, vJobStatus);
				
				// Wait 1 second before checking to limit requests
				try {
				    Thread.sleep(1000);
				}
				catch(InterruptedException ex) {
					
				    Thread.currentThread().interrupt();
				}
			}
			while(vJobState == 'R' || vJobState == 'Y' || vJobState == 'S');
		
		// Handle Errors
		} catch (java.net.MalformedURLException e) {
			e.printStackTrace();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
