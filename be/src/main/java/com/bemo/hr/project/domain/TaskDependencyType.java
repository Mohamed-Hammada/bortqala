package com.bemo.hr.project.domain;

public enum TaskDependencyType {
    FS, // Finish-to-Start
    SS, // Start-to-Start
    FF, // Finish-to-Finish
    SF  // Start-to-Finish
}
