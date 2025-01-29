package sf.hrechko.cash;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserDb extends FileDb {

    public class User {
	private String login;
	private String password;
	
	public User() {
	    login = "";
	    password = "";
	}
	
	public User(String name, String pswd) {
	    login = name;
	    password = pswd;
	}
	
	public String getLogin() {
	    return login;
	}
	
	public boolean checkPassword(String pswd) {
	    return password.equals(pswd);
	}
	
	public boolean equals(String name) {
	    return login.equals(name);
	}
	
	public boolean equals(User user) {
	    return login.equals(user.getLogin());
	}
	
	String getJsonStr() {
	    return String.format("\"%s\" : {\"password\":\"%s\"}", login, password);
	}
    }
    
    private JsonNode rootNode = null;
    private Set<User> userList = new HashSet<>();
    
    public UserDb(String filePath) {
	super(filePath);
    }
    
    public boolean init() {
	if (!load()) {
	    if (!file.exists())
		try {
		    System.out.format("Создать ");
		    file.createNewFile();
		    setStr("{}");
		    if(!save())
			return false;
		} catch (IOException e) {
		    e.printStackTrace();
		    return false;
		}
	}
	
	return loadJson();
    }
    
    public void close() {
	String outData = "{";
	int userNumber = userList.size();
	int count = 0;
	
	for (var user : userList) {
	    outData += user.getJsonStr();
	    if (++ count < userNumber) {
		outData += ",";
	    }
	}
	outData += "}";
	setStr(outData);
	save();
    }
    
    @SuppressWarnings("unlikely-arg-type")
    public boolean isUserPresent(String user) {
	return userList.contains(user);
    }
    
    public boolean isUserPresent(User user) {
	return userList.contains(user);
    }
    
    public User getUser(String name) {
	for (var user : userList) {
	    if (user.getLogin().equals(name))
		return user;
	}
	return null;
    }
    
    public boolean addUser(User newUser) {
	if (isUserPresent(newUser))
	    return false;
	
	return userList.add(newUser);
    }
    
    private boolean loadJson() {
	try {
	    rootNode = new ObjectMapper().readTree(getStr());
	    var keys = rootNode.fieldNames();
	    while(keys.hasNext()) {
		String userName = keys.next();
		var userData = rootNode.get(userName);
		String userPswd = userData.get("password").asText();
		// Todo other params
		User user = new User(userName, userPswd);
		userList.add(user);
	    }
	    return true;
	}catch (JsonMappingException e) {
	    e.printStackTrace();
	}catch(JsonProcessingException e) {
	    e.printStackTrace();
	}
	return false;
    }
    
}
