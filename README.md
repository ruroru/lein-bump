lein bump is a leiningen plugin that helps managing version in leiningen
and [lein sub](https://github.com/kumarshantanu/lein-sub) projects. It follows semantic versioning rules and allows
stepping patch, minor and major versions from cli.

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.jj/bump.svg)](https://clojars.org/org.clojars.jj/bump)

## Usage

### Retrieving the current project's version

    $ lein bump

### Setting the project's version

    $ lein bump {x.y.z}

### Stepping the project's version

    $ lein bump patch

    $ lein bump minor

    $ lein bump major

    $ lein bump dev

## License

Copyright © 2024 @ [ruroru](https://github.com/ruroru)

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
