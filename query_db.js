const mysql = require('mysql2/promise');

async function main() {
  const connection = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'itmind_inventory'
  });

  const query = process.argv[2] || "SHOW TABLES";
  const [rows] = await connection.execute(query);
  console.log(JSON.stringify(rows, null, 2));
  
  await connection.end();
}

main().catch(console.error);
