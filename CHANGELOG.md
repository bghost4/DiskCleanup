# CHANGE LOG

## 2023-03-01
    Made performance increase after scan is done.
        Too many places used Files.isRegularFile, which caused a lot of IO. This is now stored in the StatItem and gets populated in the FileScanTask
    
    TreeItemUtils has been pared down to use streamlined functions to reduce IO Usage

    
    


  