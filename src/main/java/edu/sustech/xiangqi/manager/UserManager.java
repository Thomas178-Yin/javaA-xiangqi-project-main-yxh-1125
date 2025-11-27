package edu.sustech.xiangqi.manager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String SAVE_DIR = "saves/";
    private static final String USER_FILE = SAVE_DIR + "users.dat";
    private Map<String, String> users;

    public UserManager() {
        users = new HashMap<>();
        // 【修改 2】初始化时，确保文件夹存在
        ensureSaveDirectoryExists();
        loadUsers();
    }

    // 【新增】辅助方法：创建文件夹
    private void ensureSaveDirectoryExists() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // 创建目录
        }
    }

    private void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                users = (Map<String, String>) obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("用户数据读取失败，重置为空。");
        }
    }

    private void saveUsers() {
        ensureSaveDirectoryExists();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查用户是否存在
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    /**
     * 注册新用户
     */
    public void registerUser(String username, String password) {
        users.put(username, password);
        saveUsers();
    }
}