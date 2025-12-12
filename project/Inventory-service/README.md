## 1️⃣ How to connect MySQL Workbench to this container

Your Docker Compose maps MySQL to your **host port 3307**:

```yaml
ports:
  - "3307:3306"
```

So the connection parameters for MySQL Workbench are:

| Field        | Value                             |
| ------------ | --------------------------------- |
| **Hostname** | `127.0.0.1` (or `localhost`)      |
| **Port**     | `3307`                            |
| **Username** | `inventory_user` (or `root`)      |
| **Password** | `inventory_pass` (or `mysqlroot`) |
| **Database** | `inventory_service` (optional)    |

**Steps in MySQL Workbench:**

1. Open MySQL Workbench → `+` to add a new connection.
2. Set **Connection Name** → e.g., `Inventory DB`.
3. Set **Connection Method** → `Standard (TCP/IP)`.
4. Hostname: `127.0.0.1`
5. Port: `3307`
6. Username: `inventory_user`
7. Click **Store in Vault** and enter `inventory_pass`.
8. Test connection → it should succeed.
9. Save and connect.

---

## 2️⃣ Root user vs custom user

| User Type          | Purpose                        | Access                                                                                               |
| ------------------ | ------------------------------ | ---------------------------------------------------------------------------------------------------- |
| **root**           | Superuser, default MySQL admin | Can do everything: create/drop databases, users, tables, permissions.                                |
| **inventory_user** | Custom user for your app       | Access only to `inventory_service` DB (unless granted more). Cannot modify other databases or users. |

**Why use a custom user?**

* Security: don’t give your app full control over MySQL system.
* Limits damage if credentials leak.
* Easier to manage permissions per service (important in microservices).

**Root connection** is mainly for administration, backups, or creating new users/databases.

---

💡 **Tip:** For a clean microservices setup, always use **different users for different services**. That way, `order_service` cannot touch `inventory_service` DB accidentally.

---