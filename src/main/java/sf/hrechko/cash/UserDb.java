package sf.hrechko.cash;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserDb extends FileDb {

    public class CashCategory {
	private String name;
	private double value;
	private boolean erasable = true;

	public CashCategory() {
	    name = "";
	    value = 0;
	}

	public CashCategory(String name) {
	    this.name = name;
	    value = 0;
	}

	public CashCategory(String name, double value) {
	    this.name = name;
	    this.value = value;
	}

	public String getName() {
	    return name;
	}

	public double getValue() {
	    return value;
	}

	public void setValue(double val) {
	    value = val;
	}

	public String getJsonStr() {
	    return String.format("\"%s\": \"%.02f\"", name, value);
	}

	public CashCategory setErasable(boolean state) {
	    erasable = state;
	    return this;
	}

	public boolean isErasable() {
	    return erasable;
	}
    }

    public class User {
	private String login;
	private String password;
	private Set<CashCategory> revenue = new HashSet<>(); // доход
	private Set<CashCategory> spending = new HashSet<>(); // расход

	private void createDefaultCashCategories() {
	    revenue.add(new CashCategory("Пополнение").setErasable(false));
	    revenue.add(new CashCategory("Перевод").setErasable(false));
	    spending.add(new CashCategory("Снятие").setErasable(false));
	    spending.add(new CashCategory("Перевод").setErasable(false));
	}

	private CashCategory getCategoryFromSet(String name, Set<CashCategory> set) {
	    CashCategory result = null;

	    for (var element : set) {
		if (element.getName().equals(name)) {
		    result = element;
		    break;
		}
	    }

	    return result;
	}

	public User() {
	    login = "";
	    password = "";
	    createDefaultCashCategories();
	}

	public User(String name, String pswd) {
	    login = name;
	    password = pswd;
	    createDefaultCashCategories();
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

	public boolean setIncome(CashCategory cash) {
	    CashCategory foundCategory = getIncomeByName(cash.getName());
	    if (foundCategory == null)
		return revenue.add(cash);
	    else {
		var currentValue = foundCategory.getValue() + cash.getValue();
		foundCategory.setValue(currentValue);
	    }
	    return true;
	}

	public boolean deleteIncome(CashCategory cash) {
	    CashCategory foundCategory = getIncomeByName(cash.getName());
	    if (foundCategory == null)
		return false;
	    return revenue.remove(foundCategory);
	}

	public boolean setOutcome(CashCategory cash) {
	    CashCategory foundCategory = getOutcomeByName(cash.getName());
	    if (foundCategory == null)
		return spending.add(cash);
	    else {
		var currentValue = foundCategory.getValue() + cash.getValue();
		foundCategory.setValue(currentValue);
	    }
	    return true;
	}

	public boolean deleteOutcome(CashCategory cash) {
	    CashCategory foundCategory = getOutcomeByName(cash.getName());
	    if (foundCategory == null)
		return false;
	    return spending.remove(foundCategory);
	}

	public CashCategory getIncomeByName(String name) {
	    return getCategoryFromSet(name, revenue);
	}

	public CashCategory getOutcomeByName(String name) {
	    return getCategoryFromSet(name, spending);
	}

	public String[] getIncomeNames() {
	    String[] result = new String[revenue.size()];
	    int count = 0;

	    for (var rev : revenue) {
		result[count++] = rev.getName();
	    }
	    return result;
	}

	public String[] getOutcomeNames() {
	    String[] result = new String[spending.size()];
	    int count = 0;

	    for (var sp : spending) {
		result[count++] = sp.getName();
	    }
	    return result;
	}

	public double getBalance() {
	    double balance = 0.;

	    for (var rev : revenue) {
		balance += rev.value;
	    }

	    for (var sp : spending) {
		balance -= sp.value;
	    }
	    return balance;
	}

	public String getJsonStr() {
	    String outStr = String.format("\"%s\" : {\"password\":\"%s\",\"revenue\":{", login, password);
	    int count = 0;
	    for (var rev : revenue) {
		outStr += String.format("%s", rev.getJsonStr());
		if (++count < revenue.size())
		    outStr += ",";
	    }
	    outStr += "},\"spending\":{";
	    count = 0;
	    for (var sp : spending) {
		outStr += String.format("%s", sp.getJsonStr());
		if (++count < spending.size())
		    outStr += ",";
	    }
	    outStr += "}}";
	    return outStr;
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
		    file.createNewFile();
		    setStr("{}");
		    if (!save())
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
	    if (++count < userNumber) {
		outData += ",";
	    }
	}
	outData += "}";
	setStr(outData);
	save();
    }

    public boolean isUserPresent(String user) {
	for (var usr : userList) {
	    if (usr.getLogin().equals(user))
		return true;
	}
	return false;
    }

    public boolean isUserPresent(User user) {
	return isUserPresent(user.getLogin());
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
	    while (keys.hasNext()) {
		String userName = keys.next();
		var userData = rootNode.get(userName);
		String userPswd = userData.get("password").asText();
		User user = new User(userName, userPswd);
		// Todo refactor
		var revenue = userData.get("revenue");
		var revCategories = revenue.fieldNames();
		while (revCategories.hasNext()) {
		    String revName = revCategories.next();
		    var revValue = revenue.get(revName).asText();
		    if (revValue.contains(",")) {
			revValue = revValue.replaceAll(",", ".");
		    }
		    user.setIncome(new CashCategory(revName, Double.valueOf(revValue)));
		}
		var spending = userData.get("spending");
		var spCategories = spending.fieldNames();
		while (spCategories.hasNext()) {
		    String spName = spCategories.next();
		    var spValue = spending.get(spName).asText();
		    if (spValue.contains(",")) {
			spValue = spValue.replaceAll(",", ".");
		    }
		    user.setOutcome(new CashCategory(spName, Double.valueOf(spValue)));
		}
		userList.add(user);
	    }
	    return true;
	} catch (JsonMappingException e) {
	    e.printStackTrace();
	} catch (JsonProcessingException e) {
	    e.printStackTrace();
	}
	return false;
    }

}
