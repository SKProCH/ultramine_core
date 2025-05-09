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
### Running on modern Java (11/17/21/22)

Seems like ultramine can be run on modern java without any modifications (at least on my private server it runs fine).

Here is how to do it:
1. You need to get `lwjgl3fy-forgePatches` compatible with ultramine
      - Here is prebuild version of lwjgl3fy 2.1.4: https://github.com/SKProCH/ultramine_core/releases/download/v0.4/lwjgl3ify-2.1.4-master+ea93d147db-dirty-forgePatches.jar
      - Or you can compile it by yourself from [here](https://github.com/GTNewHorizons/lwjgl3ify/)  
        You need to edit `build.gradle.kts` and change `libraryList`
        ```
            manifest {
            val libraryList = listOf(
        ```
        It should include all the libraries from the `libraries` folder, and `"forge-1.7.10-10.13.4.1614-1.7.10-universal.jar", "minecraft_server.1.7.10.jar"` should be replaced to ultramine jar name, like `"ultramine_core-server.jar"`.  
        [Here](https://gist.github.com/SKProCH/b44d24d37b6d07c04d1cd49abf3239dd#file-build-gradle-kts-L164-L229) is the example for current repo.  
        After that use `forgePatchesJar` target.
2. Rename ultramine server core jar to `"ultramine_core-server.jar"` (or whatever name you included to libraryList if you are compiled lwjgl3fy by yourself)
3. Rename `forgePatches` files to `lwjgl3ify-forgePatches.jar`
4. Create a file named `java9args.txt` with the contents of
      - [This file](https://github.com/GTNewHorizons/lwjgl3ify/blob/0fb6470e31bf13d76109cc4e2dc6239478bd69e4/java9args.txt) if you are using prebuild lwjgl3fy 2.1.4 from my releases
      - Or [this file](https://github.com/GTNewHorizons/lwjgl3ify/blob/master/java9args.txt) if you compile it by yourself from sources
5. You can now launch the server with a command like the following, assuming the first java executable on your PATH is java 11/17/21/22:
  ```shell
      java -Xmx6G -Xms6G @java9args.txt -jar lwjgl3ify-forgePatches.jar nogui
  ```

> [!IMPORTANT]
> Make sure what you launching with modern 17-22 java (not the 8 like you do before!).  
> Make sure what you downloaded mods for 17-22 java (not the 8 like you do before!).  
> Make sure what you fixed `archaicfix` and `coretweaks` as stated on top.  
> If you are using if without full GT:NH modpack, make sure that you also put `gtnhlib`,  `hodgepodge`, `lwjgl3ify` (same jar as you compiled/downloaded above) and `unimixins` in you `mods` folder.
