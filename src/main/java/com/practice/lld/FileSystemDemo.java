package com.practice.lld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSystemDemo {

    public static void main(String[] args) {
        System.out.println("To do: implementation");
    }
}

abstract class FileSystemEntry {

    String name;
    Folder parent;

    FileSystemEntry(String name) {this.name = name;}

    abstract boolean isDirectory();
    String getname() {return name;}
    void setName(String name) {this.name = name;};
    Folder getParent() {return parent;}
    void setParent(Folder parent) {this.parent = parent;}
    String getPath() {return null;}
}

class File extends FileSystemEntry {

    String content;

    File(String name, String content) {super(name); this.content = content;}

    @Override
    boolean isDirectory() {return false;}
    String getContent() {return content;}
    void  setContent(String content) {this.content = content;}
}

class Folder extends FileSystemEntry {

    Map<String, FileSystemEntry> children;

    Folder(String name) {super(name); this.children = new HashMap<>();}

    @Override
    boolean isDirectory() {return true;}
    boolean addChild(FileSystemEntry entry) {return  false;}
    FileSystemEntry removeChild(String name) {return null;}
    FileSystemEntry getChild(String name) {return null;}
    boolean hasChild(String name) {return false;}
    List<FileSystemEntry> getChildren() {return null;}
}

class FileSystem {

    FileSystemEntry root;

    FileSystem() {root = new Folder("/");}

    File createFile(String path, String content) {return null;}
    Folder createFolder(String path) {return null;}
    boolean delete(String path) {return false;}
    List<FileSystemEntry> list() {return null;}
    FileSystemEntry get(String path) {return null;}
    boolean rename(String path, String newName) {return false;}
    boolean move(String srcPath, String destPath) {return false;}
}

