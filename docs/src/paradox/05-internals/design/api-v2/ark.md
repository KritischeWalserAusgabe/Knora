<!---
Copyright © 2015-2019 the contributors (see Contributors.md).

This file is part of Knora.

Knora is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Knora is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public
License along with Knora.  If not, see <http://www.gnu.org/licenses/>.
-->

# Archival Resource Key (ARK) Identifiers

@@toc

## Requirements

Knora must produce an ARK URL for each resource. The ARK identifiers used
by Knora must respect
[the draft ARK specification](https://tools.ietf.org/html/draft-kunze-ark-18). The format of Knora’s ARK URLs must be able to change over
time, while ensuring that previously generated ARK URLs still work.

## Design

### ARK URL Format

The format of a Knora ARK URL is as follows:

```
http://HOST/ark:/NAAN/VERSION/PROJECT/RESOURCE_UUID[.TIMESTAMP]
```

- `HOST`: the hostname of the ARK resolver.

- `NAAN`: the Name Assigning Authority Number (NAAN) that the ARK resolver uses.

- `VERSION`: the version of the Knora ARK URL format being used (always 1 for now).

- `PROJECT`: the @ref:[short code](../../../03-apis/api-v2/knora-iris.md#project-short-codes) of the
  project that the resource belongs to.

- `RESOURCE_UUID`: the resource's unique ID, which is normally a
  @extref[base64url-encoded](rfc:4648#section-5) UUID, as described in
  @ref:[IRIs for Data](../../../03-apis/api-v2/knora-iris.md#iris-for-data).

- `TIMESTAMP`: an optional timestamp indicating that the ARK URL represents
  the state of the resource at a specific time in the past. The format
  of the timestamp is an [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html)
  date in Coordinated universal time (UTC), including date, time, and a 9-digit
  nano-of-second field, without the characters `-`, `:`, and `.` (because
  `-` and `.` are reserved characters in ARK, and `:` would have to be URL-encoded).
  Example: `20190118T102919000031660Z`.

Following the ARK ID spec, `/`
[represents object hierarchy](https://tools.ietf.org/html/draft-kunze-ark-18#section-2.5.1)
and `.` [represents an object variant](https://tools.ietf.org/html/draft-kunze-ark-18#section-2.5.2).
A resource is thus contained in its project, which is contained in a
repository (represented by the URL version number). A timestamp is a type of variant.

Since sub-objects are optional, there is also implicitly an ARK URL
for each project, as well as for the repository as a whole.

The `RESOURCE_UUID` is processed as follows:

1. A check digit is calculated, using the algorithm in
   the Scala class `org.knora.webapi.util.Base64UrlCheckDigit`, and appended
   to the `RESOURCE_UUID`.

2. Any `-` characters in the resulting string are replaced with `=`, because
   `base64url` encoding uses `-`, which is a reserved character in ARK URLs.

For example, given the Knora resource IRI `http://rdfh.ch/0001/0C-0L1kORryKzJAJxxRyRQ`,
and using the DaSCH's ARK resolver hostname and NAAN, the corresponding
ARK URL without a timestamp is:

```
http://ark.dasch.swiss/ark:/72163/1/0001/0C=0L1kORryKzJAJxxRyRQY
```

The same ARK URL with an optional timestamp is:

```
http://ark.dasch.swiss/ark:/72163/1/0001/0C=0L1kORryKzJAJxxRyRQY.20190118T102919000031660Z
```

### Serving ARK URLs

`SmartIri` converts Knora resource IRIs to ARK URLs. This conversion is invoked in `ReadResourceV2.toJsonLD`,
when returning a resource's metadata in JSON-LD format.

### Resolving Knora ARK URLs

A Knora ARK URL is intended to be resolved by the [Knora ARK resolver](https://github.com/dhlab-basel/ark-resolver).
