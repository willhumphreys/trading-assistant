## Read the contents of the configmap

```bash
kubectl describe configmap trading-assistant-env
```

CREATE DATABASE IF NOT EXISTS metatrader;
CREATE USER IF NOT EXISTS 'metatrader'@'%' IDENTIFIED WITH mysql_native_password BY 'password';
CREATE USER IF NOT EXISTS 'metatrader'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
GRANT ALL PRIVILEGES ON metatrader.* TO 'metatrader'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;