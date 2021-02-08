# Sample Google Drive database backup mechanism

The code contains sample mechanism of synchronization of application database content into Google Drive. 
Can be used to import and export content between user devices for specific application.

## Business logic

The application uses observer on database tables and updates local `config.json` file with table name and its last update time.

### Import operation

Import operation consists of saving local `config.json` and all json-serialized database table contents into Google Drive.

### Export operation

During the export operation the application checks whether remote (Google Drive) `config.json` already exists, and if it does - it downloads it, compares it with local `config.json` content and downloads proper database json backup files from Google Drive if each table update time is different in compared files.
