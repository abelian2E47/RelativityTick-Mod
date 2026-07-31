package net.minecraft.resource.fs;

import java.nio.file.Path;
import java.util.Map;

interface ResourceFile {
    ResourceFile EMPTY = new ResourceFile() {
        @Override
        public String toString() {
            return "empty";
        }
    };
    ResourceFile RELATIVE = new ResourceFile() {
        @Override
        public String toString() {
            return "relative";
        }
    };

    record Directory(Map<String, ResourcePath> children) implements ResourceFile {
    }

    record File(Path contents) implements ResourceFile {
    }
}

