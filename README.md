# lein-versions-plugin

A Leiningen plugin to set, update and get lein project version.

## Installation

Place `[lein-versions-plugin "1.0.0"]` into the `:plugins` vector of your project.clj.

## Usage
### set
Sets version in project.clj.

    $ lein versions-plugin set {x.y.z}
### release
#### release
Prepares version for release by removing SNAPSHOT suffix. 

    $ lein versions-plugin release
#### release minor
Prepares version for release by bumping minor version

    $ lein versions-plugin release minor
#### release major
Prepares version for release by bumping major version

    $ lein versions-plugin release major
### prepare-dev
Steps patch version and adds SNAPSHOT suffix

    $ lein versions-plugin prepare-dev
### get
Returns current project version

    $ lein versions-plugin get
## License

Copyright © 2023 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
