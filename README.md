## Ultramine 
This is ultramine core fork aimed to work with GT:NH

### Launching GT:NH

Currently you need to do 2 things:
1. Delete `archaicfix` mod from you mods folder
2. Delete `coretweaks` or edit config file `config/coretweaks.cfg` and disable `diagnostics.detect_data_watcher_id_conflicts`:
    ```cfg
    diagnostics {
      detect_data_watcher_id_conflicts {
          # Emit warning when a mod registers a typed DataWatcher object in an already occupied ID slot (vanilla only warns in the typeless registration method). [default: true]
          S:_enabled=false
  
          # Keep track of all registration stack traces, and print which ones conflict. Off by default because it adds some overhead to DataWatcher object registration. [default: false]
          B:detectDataWatcherIdConflictCulprit=false
      }
      enhance_map_storage_errors {
          # Makes MapStorage's errors more informative. [default: true]
          S:_enabled=true
      }
    }
    ```
