# Dataset model

This module contains the dependency-free dataset vocabulary shared by query producers, plugins, and execution engines.
It must not depend on a backend framework, persistence technology, or SQL implementation.

Keep types here only when they have the same meaning for every consumer. Backend storage records and SQL compilation
types remain in their respective modules and reference this shared vocabulary.
