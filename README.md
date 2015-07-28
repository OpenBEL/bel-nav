BEL Navigator
=============

Explore OpenBEL knowledge networks within Cytoscape 3.

*Capabilities*

- Configure multiple connections to OpenBEL Web API.
- Search for nodes within a knowledge network.
- Expand knowledge neighborhood of selected nodes.

  - Expand all edges
  - Expand edges using faceted search

- Show supporting evidence for the selected network edge.
- Link an entire Cytoscape network to a knowledge network.
- Validate BEL syntax for network nodes and edges.
- Knowledge network visual styles.

*Requirements*

- Cytoscape 3.x

*Installation*

**Note**: The *BEL Navigator* plugin must be installed manually (see [enhancement issue #34](https://github.com/OpenBEL/bel-nav/issues/34)).

1. Download current release from [OpenBEL/bel-nav](https://github.com/OpenBEL/bel-nav/releases).
2. Start Cytoscape.
3. Go to Apps ➡ App Manager ➡ Install from File...
4. Select the downloaded JAR and click *Install*.

*Development*

The *BEL Navigator* plugin is built with Groovy and uses a maven build system.

Additionally it includes the [gosh](https://github.com/formwork-io/gosh) shell tool to drive development functions. This shell tool is based on bash so it works within Mac, Linux, and Windows w/ Cygwin.

An isolated version of Cytoscape 3 is included in the repository to ease development. This enables plugins to automatically reload within Cytoscape after a build or test cycle succeeds.

To get started follow these steps:

1. Clone repository: `git clone git@github.com:OpenBEL/bel-nav.git`
2. Install maven.
3. Execute the `go.sh` shell script.

```bash
[tony@starship bel-nav]$ ./go.sh 

Entering go shell, [CTRL-C] to exit.
                                                                                                                                                                                
1) clean		2) build			3) test	
4) deploy		5) undeploy			6) loop (compile)	
7) loop (compile/test)	8) loop (compile/deploy)	9) loop (compile/test/deploy)	
10) start		11) debug			12) stop	
13) cytoscape log	14) groovy shell

dev / lifecycle / tools

go: 
```

3. For example to *clean build artifacts*, *build modules*, *deploy to cytoscape*, and *start cytoscape* execute:

```bash
./go.sh 1 2 4 10
```

