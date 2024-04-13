# jardiff
Compare two jars to quantify drop-in replaceability.

## Diff Kinds

| Kind                              | Names                                                            | Description                                                                                                                                                    |
|-----------------------------------|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `entry`                           | `<entryName>`                                                    | The coarsest level of Jar comparison, based on the presence or absence of named zip entries, and then based on comparison of their respective SHA-256 digests. |
| `entry.extra`                     | `<entryName>`                                                    | For resources that exist in both jars, their "extra" attributes are compared.                                                                                  |
| `manifest`                        | `META-INF/MANIFEST.MF/<Attribute-Name>`                          | Refinement for changed META-INF/MANIFEST.MF resources, comparing each of the main attributes.                                                                  |
| `manifest.section`                | `{manifest-section}/<Section-Name>/{attribute}/<Attribute-Name>` |                                                                                                                                                                |
| `osgi.header`                     | `META-INF/MANIFEST.MF/<Header-Name>`                             | Refinement for OSGi-specified Manifest Attributes                                                                                                              |
| `osgi.header.locale`              | `META-INF/MANIFEST.MF/<Header-Name>/{locale:<locale>}`           |                                                                                                                                                                |
| `osgi.header.parameter`           | `META-INF/MANIFEST.MF/<Header-Name>/<Parameter>`                 |                                                                                                                                                                |
| `osgi.header.parameter.attribute` | `META-INF/MANIFEST.MF/<Header-Name>/<Parameter>/<Attribute>`     |                                                                                                                                                                |
| `osgi.header.parameter.duplicate` | `META-INF/MANIFEST.MF/<Header-Name>/<Parameter>{1}`              |                                                                                                                                                                |
| `osgi.scr`                        | `{osgi.scr}/<Component-Name>`                                    |                                                                                                                                                                |
| `osgi.ocd`                        | `{osgi.ocd}/<Designate-Pid>`                                     |                                                                                                                                                                |


