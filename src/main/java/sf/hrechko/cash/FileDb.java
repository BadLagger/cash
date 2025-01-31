package sf.hrechko.cash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileDb {

    protected File file;
    private byte[] data;

    public FileDb(String filePath) {
	file = new File(filePath);
    }

    public boolean load() {
	try {
	    FileInputStream input = new FileInputStream(file);
	    data = input.readAllBytes();
	    input.close();
	    return true;
	} catch (IOException e) {
	    // e.printStackTrace();
	    System.out.format("Файл %s не найден!\n", file.getName());
	}
	return false;
    }

    public boolean save() {
	FileOutputStream out;
	try {
	    out = new FileOutputStream(file);
	    out.write(data);
	    out.close();
	    return true;
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return false;
    }

    public String getStr() {
	return new String(data);
    }

    public void setStr(String strData) {
	data = strData.getBytes();
    }
}
