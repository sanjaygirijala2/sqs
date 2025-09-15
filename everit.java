import java.util.*;
import java.time.LocalDateTime;
import org.json.JSONObject;
import org.json.JSONArray;
// Uncomment for Everit support:
// import org.everit.json.schema.Schema;
// import org.everit.json.schema.loader.SchemaLoader;
// import org.everit.json.schema.ValidationException;

/**
 * Notification Platform with configurable validation (Everit or Basic JSON)
 * Set USE_EVERIT = true to use Everit validation (requires library)
 * Set USE_EVERIT = false to use basic JSON validation
 */
public class SimpleNotificationPlatform {
    
    // Configuration flag - switch between Everit and basic validation
    private static final boolean USE_EVERIT = false; // Set to true if Everit library is available
    
    public static void main(String[] args) {
        // Initialize platform
        NotificationPlatform platform = new NotificationPlatform(USE_EVERIT);
        
        System.out.println("=== NOTIFICATION PLATFORM SETUP ===");
        System.out.println("Validation Mode: " + (USE_EVERIT ? "Everit JSON Schema" : "Basic JSON Validation") + "\n");
        
        // Setup domains
        setupDomains(platform);
        
        // Setup schemas (max 5 attributes each)
        setupSchemas(platform);
        
        // Setup routes
        setupRoutes(platform);
        
        // Setup capabilities
        setupCapabilities(platform);
        
        // Send workspace booking notification
        System.out.println("\n=== SENDING WORKSPACE BOOKING NOTIFICATION ===\n");
        sendWorkspaceBooking(platform);
        
        // Send invalid notification to show validation
        System.out.println("\n=== SENDING INVALID NOTIFICATION ===\n");
        sendInvalidNotification(platform);
    }
    
    private static void setupDomains(NotificationPlatform platform) {
        platform.registerDomain(new Domain(
            "mobile.push.jpmc",
            "Mobile Platform Team",
            "FCM"
        ));
        
        platform.registerDomain(new Domain(
            "desktop.rich.jpmc",
            "Desktop Platform Team",
            "WNS"
        ));
    }
    
    private static void setupSchemas(NotificationPlatform platform) {
        // Mobile schema - max 5 attributes
        String mobileSchema = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "https://jpmc.com/schemas/mobile.push.v1",
                "type": "object",
                "properties": {
                    "title": {
                        "type": "string",
                        "maxLength": 25
                    },
                    "body": {
                        "type": "string",
                        "maxLength": 50
                    },
                    "action_url": {
                        "type": "string",
                        "pattern": "^(https?://|myworkspace://)"
                    },
                    "priority": {
                        "type": "string",
                        "enum": ["low", "normal", "high", "urgent"]
                    },
                    "badge_count": {
                        "type": "integer",
                        "minimum": 0,
                        "maximum": 99
                    }
                },
                "required": ["title", "body"],
                "additionalProperties": false
            }
            """;
        
        platform.registerSchema(new SchemaDocument(
            "mobile.push.v1",
            "mobile.push.jpmc",
            mobileSchema
        ));
        
        // Desktop schema - max 5 attributes
        String desktopSchema = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "$id": "https://jpmc.com/schemas/desktop.rich.v1",
                "type": "object",
                "properties": {
                    "header": {
                        "type": "string",
                        "maxLength": 50
                    },
                    "title": {
                        "type": "string",
                        "maxLength": 100
                    },
                    "body": {
                        "type": "string",
                        "maxLength": 300
                    },
                    "footer": {
                        "type": "string",
                        "maxLength": 100
                    },
                    "icon": {
                        "type": "string",
                        "enum": ["info", "warning", "success", "calendar", "task"]
                    }
                },
                "required": ["title", "body"],
                "additionalProperties": false
            }
            """;
        
        platform.registerSchema(new SchemaDocument(
            "desktop.rich.v1",
            "desktop.rich.jpmc",
            desktopSchema
        ));
    }
    
    private static void setupRoutes(NotificationPlatform platform) {
        platform.registerRoute(new Route("mobile.myworkspace", "mobile.push.jpmc", "mobile.push.v1"));
        platform.registerRoute(new Route("desktop.myworkspace", "desktop.rich.jpmc", "desktop.rich.v1"));
    }
    
    private static void setupCapabilities(NotificationPlatform platform) {
        Capability bookSeat = new Capability("book_a_seat", "Workspace reservation");
        bookSeat.addRoute("mobile.myworkspace");
        bookSeat.addRoute("desktop.myworkspace");
        platform.registerCapability(bookSeat);
    }
    
    private static void sendWorkspaceBooking(NotificationPlatform platform) {
        // Create request matching the exact payload structure
        NotificationRequest request = new NotificationRequest();
        request.setCapability("book_a_seat");
        request.addRecipient("john.doe@jpmc.com");
        
        // Mobile payload
        NotificationPayload mobilePayload = new NotificationPayload();
        mobilePayload.setRoute("mobile.myworkspace");
        mobilePayload.setMessage(Map.of(
            "title", "Seat Reserved",
            "body", "Desk 42A booked",
            "action_url", "myworkspace://booking/123",
            "priority", "high",
            "badge_count", 1
        ));
        request.addPayload(mobilePayload);
        
        // Desktop payload
        NotificationPayload desktopPayload = new NotificationPayload();
        desktopPayload.setRoute("desktop.myworkspace");
        desktopPayload.setMessage(Map.of(
            "header", "JPMC Workspace",
            "title", "Workspace Reservation Confirmed",
            "body", "Your desk reservation for Desk 42A on Floor 3 has been confirmed",
            "footer", "Questions? Contact facilities@jpmc.com",
            "icon", "calendar"
        ));
        request.addPayload(desktopPayload);
        
        // Send notification
        platform.send(request);
    }
    
    private static void sendInvalidNotification(NotificationPlatform platform) {
        NotificationRequest request = new NotificationRequest();
        request.setCapability("book_a_seat");
        request.addRecipient("test@jpmc.com");
        
        NotificationPayload invalidPayload = new NotificationPayload();
        invalidPayload.setRoute("mobile.myworkspace");
        invalidPayload.setMessage(Map.of(
            "title", "This title is way too long for mobile and will fail validation",
            "body", "Test",
            "badge_count", 999,  // Exceeds maximum of 99
            "priority", "invalid_priority"  // Invalid enum value
        ));
        request.addPayload(invalidPayload);
        
        platform.send(request);
    }
}

// Main Platform Class
class NotificationPlatform {
    private Map<String, Domain> domains = new HashMap<>();
    private Map<String, SchemaDocument> schemas = new HashMap<>();
    private Map<String, Route> routes = new HashMap<>();
    private Map<String, Capability> capabilities = new HashMap<>();
    private SchemaValidator validator;
    
    public NotificationPlatform(boolean useEverit) {
        this.validator = new SchemaValidator(useEverit);
    }
    
    public void registerDomain(Domain domain) {
        domains.put(domain.getId(), domain);
        System.out.println("✓ Registered domain: " + domain.getId());
    }
    
    public void registerSchema(SchemaDocument schema) {
        schemas.put(schema.getId(), schema);
        validator.compileSchema(schema.getId(), schema.getJsonSchema());
        System.out.println("✓ Registered schema: " + schema.getId());
    }
    
    public void registerRoute(Route route) {
        routes.put(route.getId(), route);
        System.out.println("✓ Registered route: " + route.getId() + " → " + route.getSchemaId());
    }
    
    public void registerCapability(Capability capability) {
        capabilities.put(capability.getId(), capability);
        System.out.println("✓ Registered capability: " + capability.getId());
    }
    
    public void send(NotificationRequest request) {
        System.out.println("Processing notification:");
        System.out.println("  Capability: " + request.getCapability());
        System.out.println("  Recipients: " + request.getRecipients());
        
        Capability capability = capabilities.get(request.getCapability());
        if (capability == null) {
            System.out.println("❌ Unknown capability: " + request.getCapability());
            return;
        }
        
        for (NotificationPayload payload : request.getPayloads()) {
            System.out.println("\n  Route: " + payload.getRoute());
            
            Route route = routes.get(payload.getRoute());
            if (route == null) {
                System.out.println("  ❌ Unknown route");
                continue;
            }
            
            if (!capability.supportsRoute(payload.getRoute())) {
                System.out.println("  ❌ Route not supported by capability");
                continue;
            }
            
            SchemaDocument schema = schemas.get(route.getSchemaId());
            if (schema == null) {
                System.out.println("  ❌ Schema not found");
                continue;
            }
            
            System.out.println("  Schema: " + schema.getId());
            
            // Validate message
            ValidationResult result = validator.validate(schema.getId(), payload.getMessage());
            
            if (result.isValid()) {
                System.out.println("  ✓ Validation passed");
                
                Domain domain = domains.get(route.getDomainId());
                System.out.println("  Delivering via: " + domain.getId() + " (" + domain.getPlatformType() + ")");
                
                // Display message
                displayMessage(payload.getRoute(), payload.getMessage());
            } else {
                System.out.println("  ❌ Validation failed:");
                for (String error : result.getErrors()) {
                    System.out.println("    - " + error);
                }
            }
        }
    }
    
    private void displayMessage(String route, Map<String, Object> message) {
        if (route.startsWith("mobile")) {
            System.out.println("\n  📱 Mobile Notification:");
            System.out.println("     Title: " + message.get("title"));
            System.out.println("     Body: " + message.get("body"));
            if (message.get("action_url") != null) {
                System.out.println("     Action: " + message.get("action_url"));
            }
        } else if (route.startsWith("desktop")) {
            System.out.println("\n  💻 Desktop Notification:");
            if (message.get("header") != null) {
                System.out.println("     Header: " + message.get("header"));
            }
            System.out.println("     Title: " + message.get("title"));
            System.out.println("     Body: " + message.get("body"));
            if (message.get("footer") != null) {
                System.out.println("     Footer: " + message.get("footer"));
            }
        }
    }
}

// Schema Validator with flag for Everit or Basic validation
class SchemaValidator {
    private boolean useEverit;
    private Map<String, Object> compiledSchemas = new HashMap<>();
    
    public SchemaValidator(boolean useEverit) {
        this.useEverit = useEverit;
    }
    
    public void compileSchema(String schemaId, String jsonSchema) {
        try {
            if (useEverit) {
                // Everit compilation (uncomment when library is available)
                // JSONObject schemaJson = new JSONObject(jsonSchema);
                // Schema schema = SchemaLoader.load(schemaJson);
                // compiledSchemas.put(schemaId, schema);
                System.out.println("  [Everit mode - schema would be compiled]");
            } else {
                // Basic JSON compilation
                compiledSchemas.put(schemaId, new JSONObject(jsonSchema));
            }
        } catch (Exception e) {
            System.err.println("Failed to compile schema: " + e.getMessage());
        }
    }
    
    public ValidationResult validate(String schemaId, Map<String, Object> message) {
        try {
            if (useEverit) {
                return validateWithEverit(schemaId, message);
            } else {
                return validateWithBasicJson(schemaId, message);
            }
        } catch (Exception e) {
            return new ValidationResult(false, Arrays.asList("Validation error: " + e.getMessage()));
        }
    }
    
    private ValidationResult validateWithEverit(String schemaId, Map<String, Object> message) {
        // Everit validation (uncomment when library is available)
        /*
        try {
            JSONObject messageJson = new JSONObject(message);
            Schema schema = (Schema) compiledSchemas.get(schemaId);
            
            if (schema == null) {
                return new ValidationResult(false, Arrays.asList("Schema not found: " + schemaId));
            }
            
            schema.validate(messageJson);
            return new ValidationResult(true, new ArrayList<>());
            
        } catch (ValidationException e) {
            List<String> errors = new ArrayList<>();
            errors.add(e.getMessage());
            
            // Get all validation errors
            e.getCausingExceptions().forEach(ve -> {
                errors.add(ve.getMessage());
            });
            
            return new ValidationResult(false, errors);
        }
        */
        
        // Placeholder when Everit is not available
        System.out.println("  [Everit validation would run here]");
        return validateWithBasicJson(schemaId, message);
    }
    
    private ValidationResult validateWithBasicJson(String schemaId, Map<String, Object> message) {
        JSONObject messageJson = new JSONObject(message);
        JSONObject schemaJson = (JSONObject) compiledSchemas.get(schemaId);
        
        if (schemaJson == null) {
            return new ValidationResult(false, Arrays.asList("Schema not found: " + schemaId));
        }
        
        List<String> errors = new ArrayList<>();
        
        // Check required fields
        JSONArray required = schemaJson.optJSONArray("required");
        if (required != null) {
            for (int i = 0; i < required.length(); i++) {
                String field = required.getString(i);
                if (!messageJson.has(field)) {
                    errors.add("Missing required field: " + field);
                }
            }
        }
        
        // Check properties
        JSONObject properties = schemaJson.optJSONObject("properties");
        if (properties != null) {
            // Check each field in the message
            for (String key : messageJson.keySet()) {
                // Check if additional properties are allowed
                if (!properties.has(key)) {
                    if (!schemaJson.optBoolean("additionalProperties", true)) {
                        errors.add("Additional property not allowed: " + key);
                    }
                    continue;
                }
                
                JSONObject prop = properties.getJSONObject(key);
                Object value = messageJson.get(key);
                
                // Type validation
                if (prop.has("type")) {
                    String expectedType = prop.getString("type");
                    if (!validateType(value, expectedType)) {
                        errors.add("Field '" + key + "' has wrong type. Expected: " + expectedType);
                        continue;
                    }
                }
                
                // String validation
                if (value instanceof String) {
                    String str = (String) value;
                    
                    // Check maxLength
                    if (prop.has("maxLength")) {
                        int maxLength = prop.getInt("maxLength");
                        if (str.length() > maxLength) {
                            errors.add("Field '" + key + "' exceeds maxLength of " + 
                                maxLength + " (actual: " + str.length() + ")");
                        }
                    }
                    
                    // Check minLength
                    if (prop.has("minLength")) {
                        int minLength = prop.getInt("minLength");
                        if (str.length() < minLength) {
                            errors.add("Field '" + key + "' below minLength of " + minLength);
                        }
                    }
                    
                    // Check pattern
                    if (prop.has("pattern")) {
                        String pattern = prop.getString("pattern");
                        if (!str.matches(pattern)) {
                            errors.add("Field '" + key + "' doesn't match pattern: " + pattern);
                        }
                    }
                    
                    // Check enum
                    if (prop.has("enum")) {
                        JSONArray enumValues = prop.getJSONArray("enum");
                        boolean found = false;
                        for (int i = 0; i < enumValues.length(); i++) {
                            if (enumValues.getString(i).equals(str)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            errors.add("Field '" + key + "' value '" + str + 
                                "' not in allowed values: " + enumValues.toString());
                        }
                    }
                }
                
                // Integer validation
                if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    
                    // Check minimum
                    if (prop.has("minimum")) {
                        int min = prop.getInt("minimum");
                        if (intValue < min) {
                            errors.add("Field '" + key + "' below minimum: " + min);
                        }
                    }
                    
                    // Check maximum
                    if (prop.has("maximum")) {
                        int max = prop.getInt("maximum");
                        if (intValue > max) {
                            errors.add("Field '" + key + "' exceeds maximum: " + max + 
                                " (actual: " + intValue + ")");
                        }
                    }
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private boolean validateType(Object value, String expectedType) {
        switch (expectedType) {
            case "string":
                return value instanceof String;
            case "integer":
                return value instanceof Integer;
            case "number":
                return value instanceof Number;
            case "boolean":
                return value instanceof Boolean;
            case "array":
                return value instanceof List || value instanceof JSONArray;
            case "object":
                return value instanceof Map || value instanceof JSONObject;
            default:
                return true;
        }
    }
}

// Domain Model
class Domain {
    private String id;
    private String ownerTeam;
    private String platformType;
    
    public Domain(String id, String ownerTeam, String platformType) {
        this.id = id;
        this.ownerTeam = ownerTeam;
        this.platformType = platformType;
    }
    
    public String getId() { return id; }
    public String getOwnerTeam() { return ownerTeam; }
    public String getPlatformType() { return platformType; }
}

// Schema Document
class SchemaDocument {
    private String id;
    private String domainId;
    private String jsonSchema;
    
    public SchemaDocument(String id, String domainId, String jsonSchema) {
        this.id = id;
        this.domainId = domainId;
        this.jsonSchema = jsonSchema;
    }
    
    public String getId() { return id; }
    public String getDomainId() { return domainId; }
    public String getJsonSchema() { return jsonSchema; }
}

// Route Model
class Route {
    private String id;
    private String domainId;
    private String schemaId;
    
    public Route(String id, String domainId, String schemaId) {
        this.id = id;
        this.domainId = domainId;
        this.schemaId = schemaId;
    }
    
    public String getId() { return id; }
    public String getDomainId() { return domainId; }
    public String getSchemaId() { return schemaId; }
}

// Capability Model
class Capability {
    private String id;
    private String description;
    private Set<String> supportedRoutes = new HashSet<>();
    
    public Capability(String id, String description) {
        this.id = id;
        this.description = description;
    }
    
    public void addRoute(String route) {
        supportedRoutes.add(route);
    }
    
    public boolean supportsRoute(String route) {
        return supportedRoutes.contains(route);
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
}

// Notification Request
class NotificationRequest {
    private String capability;
    private List<String> recipients = new ArrayList<>();
    private List<NotificationPayload> payloads = new ArrayList<>();
    
    public void setCapability(String capability) {
        this.capability = capability;
    }
    
    public void addRecipient(String recipient) {
        recipients.add(recipient);
    }
    
    public void addPayload(NotificationPayload payload) {
        payloads.add(payload);
    }
    
    public String getCapability() { return capability; }
    public List<String> getRecipients() { return recipients; }
    public List<NotificationPayload> getPayloads() { return payloads; }
}

// Notification Payload
class NotificationPayload {
    private String route;
    private Map<String, Object> message;
    
    public void setRoute(String route) {
        this.route = route;
    }
    
    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }
    
    public String getRoute() { return route; }
    public Map<String, Object> getMessage() { return message; }
}

// Validation Result
class ValidationResult {
    private boolean valid;
    private List<String> errors;
    
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }
    
    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
}
