# CHANGE LOG

## 2023-03-01
    Made performance increase after scan is done.
        Too many places used Files.isRegularFile, which caused a lot of IO. This is now stored in the StatItem and gets populated in the FileScanTask
    
    TreeItemUtils has been pared down to use streamlined functions to reduce IO Usage
 
    Reordered Search Strategies to be more Logical

    Added Status Label at bottom of screen to give user better idea of what is being processed

    Sorted Items in File Owners, File Extensions, and File Types

    fixed treemap showing stale data when scanning folder
    
    TODO File Type and File Ext need to be some kind of autocomplete TextField rather than a ComboBox, because the list can be too extensive

    
  