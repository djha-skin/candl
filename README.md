# CANDL

```
Convenient
Arbitrarily
Nestable
Document
Language
```

## Example

```
# The data in this document is fictitious.
# Note that for schema validation puroses, a single constraint at the
# top level is often more than sufficient. I don't put constraints on
# every single thing in this document.
=person {
    name "Daniel"
    age 42
    will >
      >This is my last will and
      >testament.

    poem |
      |So I went down to the codes one day
      |To the lonely C and the Py;
      |And all I ask is a terminal
      |And a browser to sail 'er by.
      |

    # Keywords are an interesting way to represent enums.
    eyes *eye-color/brown
    height 194.6
    # Best not reveal my age, commented out
    #born =rfc3339 "1986-01-07T04:13:00.0000-07:00"

    kids [
        # These are also person objects but the reader knows this
        # already due to the top `=person` constraint. So they need not
        # be constrained here.
        { name "Bobby" }
        { name "Jill" }
    ]
    pets [
        # pets can be dogs or cats.
        # polymorphism in schema suggests constraints could help here.
        =dog {
          name "Rover"
          breed "Goldendoodle"
        }
        =cat {
          name "Kibble"
          pelt "tabby"
        }

    ]
    sanity null
    happy true
    dead false
}
```

## Motivation

I recently a blog post by Drew DeVault making a [request for a new
YAML](https://drewdevault.com/2021/07/28/The-next-YAML.html) and I
thought I'd take a crack at it.

In particular I wish to address DeVault's request for embedding
documents inside documents. I too agree that this is the killer feature
of YAML.

### Things we want to avoid

I also agree that the un-feature of YAML is how complicated it is. We
don't need [63 different types of multi-line
strings](https://stackoverflow.com/a/21699210), and we don't need
entities.

I also don't like all the commas and quotation marks in JSON, they're
annoying.

And I feel like white space indentation with semantic meaning
complicates things. It makes it difficult for IDEs to tell where to
indent the next line, and it complicates the copy paste experience. It
also leaves room for weird edge cases in different parsers. Even if it's
doesn't, I just don't like it. However, I do agree that using white
space as a delimiter instead of commas or colons is much easier on the
eyes.

## Design Goals

- Design a new data language to replace YAML
- If I'm honest, I kind of want it to be good enough to replace
  [EDN](https://github.com/edn-format/edn/blob/master/README.md), too. I
  work in Clojure a lot. While I like EDN, it's really custom molded to
  Clojure without regard for other languages. I'd like a format that
  makes my Clojure work more interoperable.
- Its primary use case will be manually edited, machine-read files. But
  it should work okay for machine/machine as well.
- The syntax must not rely on indentation.
- It must be interoperable between different host parsing languages.
- It must be able to nest embedded documents with the same syntax as the
  embedded document without any escaping arbitrarily deeply.
- The syntax must be familiar and easy to understand.
- The syntax must not be overly verbose.
- The syntax should not be able to represent the same type of thing in
  multiple different ways.
- Unless it needs to.
- It should support statically typed languages better than YAML.
- It must support the following languages for maximum interoperability:
  Python, Ruby, Clojure, a Golang, and C. I have history with all of
  these languages and I care about them.
- It need not be a superset of JSON. (But I did it anyway.)

## Prior Art

The main inspirations for this specification came from
[JSON](https://trac.ietf.org/trac/json/browser/abnf/json.abnf?rev=2),
[YAML](https://yaml.org/spec/1.2.2/), [text email block quoting
conventions](https://en.m.wikipedia.org/wiki/Posting_style#Quoted_line_prefix),
and Rich Hickey's
[EDN](https://github.com/edn-format/edn/blob/master/README.md). White
space (but not indentation) as a delimiter came from EDN.

Scala has something similar to the block quotes syntax presented herein
with their [`stripMargin`
method](https://www.scala-lang.org/api/2.12.7/scala/collection/immutable/StringLike.html#stripMargin:String).
I discovered this after I made it up, but I thought I'd acknowledge it
anyway.

Constraints were partially a inspired by EDN's tags (like `#inst`.)

## Specification

I'm going to keep it mostly informal here, but also do often get into
the nitty gritty.

### Unicode Ready

Documents are encouraged to be encoded in UTF-8. To support machine to
machine better, this is not required. However, the entire document is
defined to be a Unicode string, with no unprintable characters in it. As
ASCII characters are often used in the spec, an encoding that supports
them is a must. Encodings that come to mind are UTF-7, UTF-8, UTF-16,
Windows UTF-16, UTF-32, ISO-8859-1 and ASCII itself.

The grammar explicitly contains provision for encountering a byte order
mark at the beginning of a document and ignoring it, just in case. It
has caused me too much pain not to be explicitly watched for (looking at
you, Windows).

### JSON superset

CANDL is a mostly a superset of JSON simply because that makes
describing it easier. Also, I like most design decisions in JSON. It
isn't a perfect superset in the sense that YAML isn't: edge cases
supported by JSON might not be supported by CANDL such as duplicate keys
in an object, and spaces surrounding a single primitive if that's the
whole document.

### New Token: identifier

A new token is here introduced which will be used further on in the
document: identifiers.

A CANDL identifier either a simple name or a surname and name, in that
order, separated by a forward slash (`/`). If the identifier is a
surname/name pair, it is said to be a "fully qualified identifier".

A name is one or more sub-identifiers separated by dashes (`-`) or dots
(the ASCII period `.`)

A sub-identifier is a sequence of Unicode code points. The first code
point is an alphabetical ASCII character, an underscore, or any other
non-ASCII character. Subsequent code points can include ASCII
alphabetical characters, ASCII numerical characters, underscores, the
question mark (`?`), the exclamation mark (`!`), or any other non-ASCII
Unicode code point.

For JSON compatibility and to dispel confusion, identifiers are not and
can't be the strings `null`, `true`, or `false`.

### Character Class: whitespace and blankspace

Whitespace and blankspace is often talked about in the document. In this
document, "blankspace" is a character class that refers to any one of
three ANSI characters: the vertical tab character, the tab character,
and the space. The word "whitespace" refers to a character class
containing any of the aforementioned characters as well as the carriage
return, andnewline ANSI characters. Together with other characters,
whitespace and blankspace often helps form different tokens in the
language.

### block quote Syntax

CANDL introduces a new syntax for multi-line strings known as block
quote syntax.

There are two types of block quotes, the verbatim block quote and the
prose block quote.

Both types of block quotes use a *block mark* to start the block and
denote its continuation. All block marks found in any block quote must
be the same for the whole block.

The block mark for the verbatim block quote is the pipe character (`|`),
while the block mark for the prose block quote is the greater than sign
(`>`).

A block quote starts with a block mark, zero or more blankspace
characters and a line delimiter. These are discarded as delimiter front
matter and not included in the resultant string.

Each subsequent line in the block quote starts with zero or more
blankspace characters followed by the block mark. These blankspace
characters and block mark together are called the block quote's
*prefix*. This prefix is discarded for each line when building the
resultant string.

A block quote ends when an empty line is encountered. This empty line,
its line delimiter, and the line delimiter of the previous line is
discarded when building the resultant string.

Prose block quotes have the further constraint that any substring found
in the resultant string consisting of all whitespace characters that lie
between two non whitespace Unicode code points are replaced with a
single space character.

Some examples.

This results in the empty string:

```
|
|

```

This returns `" hello, how are you?  "`:

```
>
> hello, how are
>     you?

```

This returns `"hello, it's me"`

```
  >
  >hello, it's me

```

This example shows how to ensure the block quote encodes an ending line
delimiter by simply including an extra one at the end of a quote. The
following listing directly translates to this JSON string: `"two\n
lines\n"`

```
|
|two
| lines
|

```

This allows the writer to control whitespace at the beginning and end of
a string while also making the document very readable and limiting the
number of ways to write multi-line strings to just two. It also makes
things easier on the parser.

### Comments

Comments are start with a hash symbol, followed by anything other than a
line delimiter, followed by a single line delimiter. The entire comment
(including line delimiter) must be discarded from the document by the
parser as if it never existed, its contents left uninterpreted.

They can appear on the same line as data or other interpreted content.
They may be written interspersing prose or verbatim lines, so long as
they are on their own line and the only characters before the comment
comprises blankspace.

Except for prose and verbatim lines and their terminating blank lines,
pretty much anywhere that white space can appear, so can a comment. See
the ABNF at the end for more details.

Here is an example:

```
{
   story:
   # I can put a comment
   # between keys and values,
   | # on the start of block
   # quotes, interspersed
   # between
   |Listen my children, and you shall hear
   # block quote lines, or even
   |Of the midnight ride of Paul Revere.
   # between the block quote and
   # its terminating blank line
# ...
}
```

### Symbols and Keywords

CANDL introduces two new primitive types in addition to the basic JSON
types. Symbols are [interned
strings](https://en.m.wikipedia.org/wiki/String_interning) that refer to
something. Keywords are symbols that [refer to
themselves](https://github.com/edn-format/edn/blob/master/README.md#keywords).
They're an interesting way to represent e.g. C enum values, for example.
If the backing parser isn't interested in symbols or keywords they can
just treat them as strings and/or enums.

`symbol`

Symbols are simply bare word CANDL identifiers that can't be `true`,
`null` or `false`.

`*keyword`

Keywords are identifiers with a prepended asterisk (`*`).

Symbols may be used instead of strings in keys of objects, but not as
values.

Keywords may be used as values but not as keys in objects.

If the parser's host language does not have a notion of a keyword, they
should interpret them as an enum option. They may throw an error if the
enum type in question has not been pre-registered with the parser and so
is unknown to it. Symbols must be interpreted as a string when used as
keys in an object if the language has no notion of a symbol. They may be
interpreted as either a symbol or keyword if the language supports it.

### Arrays

Arrays start with a `[` and end with a `]`. The delimiter for lists is a
token that consists of a non-empty string of whitespace characters or
the comma (`,`). Delimiters may appear before the first item in the list
or after the last one without issue.

### Objects

The character `{` starts the map and the character `}` finishes it.
Separation between keys and values happens via a token which is a non
empty string consisting either of whitespace characters, or the colon
character (`:`). The delimiter for different key/value pairs is a token
that consists of a non-empty string of whitespace characters or the
comma (`,`). Delimiters may appear before the first pair in the object
or after the last one without issue.

Duplicate keys within an object are not supported and may cause a parser
error. All keys within a given map must be either symbols or strings.
Keys can be symbols or strings. Order of presentation of the key value
pairs must not matter.

### Constraints

CANDL elements can be tagged with a constraint. This is an equals sign
(`=`) followed by a CANDL identifier with no spaces in between the equal
sign and the identifier. One or more blankspace characters separate this
constraint and the value it takes as its argument, with no comments or
line delimiters in between the constraints and the value that it
constrains. The constraint comes first. At most one constraint is
allowed per value. Identifiers that are not fully qualified are
reserved, and so must either refer to a built-in constraints or cause an
error.

Example:

```
# wrong: no spaces between equals and constraint identifier allowed
= square [1,2]

# wrong: there must be a space between the constraint identifier and
# the element
=square[1,2]

#right
=square [1,2]
```

Constraints are provided as a tool for schema and data validation.

Writers may constrain data in ways that readers might expect.

There are a set of built-in constraints defined.

- `=u8`, `=u16`, `=u32`, `=u64`, `=u128`, or `=ubig` means the following
  number must be able to fit into 8, 16, 32, 64, 128, or an arbitrary
  number of bits in unsigned integer space and it must be a non-negative
  number.
- `=s8`, `=s16`, `=s32`, `=s64`, or `=sbig` means the following number
  must be able to fit into 8, 16, 32, 64, or an arbitrary number of bits
  in twos complement signed integer space.
- `=f32`, `=f64`, `=f128` or `=fbig` means that the following number
  must be able to be loslessly represented using a 32, 64, 128 or an
  arbitrary number of bits in an IEEE 754 floating point representation.
- `=rfc3339` means the following string must conform to an RFC 3339
  date.
- `=base64` means the following string is a base64 encoding of binary
  data.
- `=email` means the following string must be a valid email.
- `=url` means the following string must be a valid URL.

Other constraints can be predefined and pre-registered with the parser.
User-defined constraints must be fully qualified. These constraints
would be published as part of the API of the reading program. Known
constraints should always be enforced. The constraints in this document
should be enforced. Other, pre-registered constraints known to the
parser should also be enforced. Constraints unknown to the parser must
cause a parsing error.

Constraints may be written by the writer but they may also be expected
by the reader. Failure to add a constraint to an element in a document
where is it is expected by the parser may cause a parsing error. The
parts are may expect any number of possible constraints on a particular
element, or none.

They are presented as a way to solve the schema problem without getting
in the way of untyped languages such as Python that don't necessarily
need this. They should probably be used sparingly but are present if
needed.


## Observations

- CANDL is not a subset of any language. That's not a bug, that's a
  feature.

- You can embed a doc inside a doc inside a doc ad nauseum, like you can
  in YAML, using verbatim block quotes.

- The parser won't get tripped up by e.g. tab characters like YAML does

- Schema enforcement is supported at the parser level using constraints.

- Constraints do not guarantee or even say anything about how the data
  will be stored in the host language, they simply make checkable
  statements about the data itself and lets the parser do what it wants
  with that information. This allows schemas to be checked without
  sacrificing interoperability.

- Bareword keys is something I wanted to support. Using symbols, it's
  there.

- Doesn't support all possible EDN documents, but lots of them. Also
  makes choices that allow for much better interoperability.

- It still manages to be a superset of JSON. DeVault didn't need this
  but I think it's pretty cool.

# ABNF

The following ABNF is released under the terms of the BSD license,
produced here:

> Copyright (c) 2022, Daniel Jay Haskin
> All rights reserved.

> Redistribution and use in source and binary forms, with or without
> modification, are permitted provided that the following conditions are
> met:
>
> 1. Redistributions of source code must retain the above copyright
>    notice, this list of conditions and the following disclaimer.
>
> 2. Redistributions in binary form must reproduce the above copyright
>    notice, this list of conditions and the following disclaimer in the
>    documentation and/or other materials provided with the
>    distribution.
>
> 3. Neither the name of the copyright holder nor the names of its
>    contributors may be used to endorse or promote products derived
>    from this software without specific prior written permission.
>
> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
> "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
> LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
> A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
> HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
> SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
> LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
> DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
> THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
> (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
> OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The following ABNF is a derivative work of the [JSON
ABNF](https://trac.ietf.org/trac/json/browser/abnf/json.abnf?rev=2)
published by the IETF. It was accessed at that page on July 13th 2022
and was edited by jhildebr on October 6, 2013.

The RFCs where the ABNF from which the following ABNF is derived is
comes from the following:

- [RFC8258](https://datatracker.ietf.org/doc/html/rfc8259#section-2) by
  T. Bray, Ed. of Textuality, December 2017.
- [RFC5234](https://datatracker.ietf.org/doc/html/rfc5234) by D.
  Crocker, Ed. Of Brandenburg InternetWorking and P. Overell of THUS
  plc., January 2008.

The work from which this ABNF is derived is [released by the IETF under
the terms of the BSD
license](https://trustee.ietf.org/wp-content/uploads/IETF-TLP-1.pdf)
(see section 4c of that document). The license for their code is
reproduced here:

> Copyright (c) 2008, 2013 and 2017 IETF Trust and the persons
> identified as the document authors. All rights reserved.
>
> Redistribution and use in source and binary forms, with or without
> modification, are permitted provided That the following conditions are
> met:
>
> - Redistributions of source code must retain the above copyright
>   notice, this list of conditions and the following disclaimer.
> - Redistributions in binary form must reproduce the above copyright
>   notice, this list of conditions and the following disclaimer in the
>   documentation and/or other materials provided with the distribution.
> - Neither the name of Internet Society, IETF or IETF Trust, nor the
>   names of specific contributors, may be used to endorse or promote
>   products derived from this software without specific prior written
>   permission.
>
> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
> "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
> LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
> A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
> OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
> SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
> LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
> DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
> THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
> (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
> OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The code in question which is derived from the IETF's work begins after
this paragraph and ends at the end of this document.

```
CANDL-text = *1%xFEFF  ; bom check
             *( ws
                *comment )
             value
             *( ws
                *comment )

value = [ constraint 1*blank ]
        ( true
        / false
        / null
        / number
        / keyword
        / string
        / prose
        / verbatim
        / object
        / array )

; Constraints
constraint = begin-constr
             identifier
begin-constr = %x3d    ; =

; True
true = %x74.72.75.65   ; true

; False
false = %x66.61.6C.73.65
                       ; false

; Null
null = %x6E.75.6C.6C   ; null

; Number section
number = [ minus ]
         int
         [ frac ]
         [ exp ]
decimal-point = %x2E   ; .
digit1-9 = %x31-39     ; 1-9
e = %x65               ; e
  / %x45               ; E
exp = e
      [ minus
      / plus ]
      1*DIGIT
frac = decimal-point
       1*DIGIT
int = zero
    / ( digit1-9
        *DIGIT )
minus = %x2D            ; -
plus = %x2B             ; +
zero = %x30             ; 0
; DIGIT equivalent to DIGIT rule
; in [RFC5234]
DIGIT = %x30-39         ; 0-9

; Keyword section
keyword = begin-keyword
          identifier
begin-keyword = %x2A    ; *

; String section
string = quotation-mark
         *char
         quotation-mark
char = unescaped
     / escape
     / ( %x22           ; ":U+0022
       / %x5C           ; \:U+005C
       / %x2F           ; /:U+002F
       / %x62           ; b:U+0008
       / %x66           ; f:U+000C
       / %x6E           ; n:U+000A
       / %x72           ; r:U+000D
       / %x74           ; t:U+0009
       / %x75 4HEXDIG ) ; uXXXX:
                        ;   U+XXXX
escape = %x5C           ; \
quotation-mark = %x22   ; "
unescaped = %x20-21     ; all
          / %x23-5B     ; except
          / %x5D-10FFFF ; " and \
; HEXDIG equivalent to HEXDIG rule
; in [RFC5234]
HEXDIG = DIGIT.         ; 0-9
       / %x41-46        ; A-F
       / %x61-66        ; a-f

; Prose section
prose = begin-prose
        1*prose-line
        blank-line
begin-prose = prose-mark
              ignore
              line-delimiter
ignore = blankspace
         *( comment
            blankspace )

prose-mark = %x3E      ; >
prose-line = ignore
             prose-mark
             line-content
             line-delimiter
blank-line = ignore
             line-delimiter

; Verbatim section
verbatim = begin-verbatim
           1*verbatim-line
           blank-line
begin-verbatim = verbatim-mark
                 ignore
                 line-delimiter
verbatim-mark = %x7C   ; |
verbatim-line = ignore
                verbatim-mark
                line-content
                line-delimiter

; Objects
object = begin-object
         [ [ value-sep ]
           member
           *( value-sep
              member )
           [ value-sep ] ]
         end-object
begin-object = %x7B ws ; {
val-delim = ws
          / %x2C       ; ,
value-sep = 1*( val-delim
                *comment )
member = key
         name-sep
         value
key = symbol
    / string
symbol = identifier
name-delim = ws
           / %x3A )    ; :
name-sep = 1*( name-delim
               *comment )
end-object = %x7D      ; }

; Arrays
array = begin-array
        [ [ value-sep ]
          value
          *( value-sep
             value )
          [ value-sep ] ]
        end-array
begin-array = %x5B     ; [
end-array = %x5D       ; ]

; Identifiers section
identifier = name
           / ( name
               id-name-delim
               name )
name = sub-id
       *( sub-id-delim
          sub-id )
sub-id = begin-id
         *middle-id
begin-id = %x5F        ; _
         / %x41-%5A    ; A-Z
         / %x61-%7A    ; a-z
         / %80-%10FFF  ; non-ASCII
middle-id = %x5F       ; _
          / %x21       ; !
          / %x3F       ; ?
          / %30-%39    ; 0-9
          / %x41-%5A   ; A-Z
          / %x61-%7A   ; a-z
          / %80-%10FFF ; non-ASCII
sub-id-delim = %x2D    ; -
             / %x2E    ; .
id-name-delim = %x2F.  ; /

; Rules shared between different
; sections
blank = %x20           ; Space
      / %x09           ; \t
ws = blank
   / line-delimiter
line-delimiter = %x0D %x0A
                       ; \r\n
               / %x0A  ; \n
               / %0D.  ; \r
comment = comment-start
          line-content
          line-delimiter
comment-start = %x23   ; #
line-content = %x09    ; all
             / %x20-5B ; but
             / %x5D-10FFF
                       ; \r and \n
```