package de.bwl.bwfla.imagearchive.util;

import de.bwl.bwfla.api.imagearchive.Alias;
import de.bwl.bwfla.api.imagearchive.Entry;
import de.bwl.bwfla.api.imagearchive.ImageNameIndex;

public class EmulatorRegistryUtil {


    private static Entry _getEntry(ImageNameIndex index, String name, String version) {
        for (ImageNameIndex.Entries.Entry _entry : index.getEntries().getEntry()) {
            Entry indexEntry = _entry.getValue();
            if (!indexEntry.getName().equals(name))
                continue;

            if (version == null)
                return indexEntry;

            if (indexEntry.getVersion().equals(version))
                return indexEntry;
        }
        return null;
    }


    public static Entry getEntry(ImageNameIndex index, String name, String version) {
        Entry result = _getEntry(index, name, version);
        if (result != null)
            return result;

        if (version == null)
            version = "latest";

        for (ImageNameIndex.Aliases.Entry _entry : index.getAliases().getEntry()) {
            Alias indexAlias = _entry.getValue();
            if (!indexAlias.getName().equals(name))
                continue;

            if (indexAlias.getAlias().equals(version))
                return _getEntry(index, name, indexAlias.getVersion());
        }

        return null;
    }
}
