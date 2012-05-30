The goal of this project is to make it as easy as possible
to construct test data, including hierarchical data with
constraints on how datum X can be used with datum
Y. 

[User documentation](https://github.com/marick/peano/wiki)

## Easy lookup of named tabular data

So, for example, suppose I have animals and medical procedures:

```clojure
(data [animal :by :name :with-selectors]
      {:name "betty" :species :bovine :legs 4}
      {:name "hank" :species :equine :legs 4}
      {:name "jake" :species :equine :legs 3} ; poor jake
      {:name "dawn" :species :human :legs 2})

(data [procedure :by :name :with-selectors]
      {:name "hoof trim" :species-rule :equine-only :days-delay 0}
      {:name "superovulation" :species-rule :bovine-only :days-delay 90}
      {:name "physical exam" :species-rule :all :days-delay 0})
```

From this, we can auto-construct *selectors* that let us
easily ask for the names of data that satisfy constraints.

```clojure
  (animal?> :legs 4) ;; ["betty" "hank"]
  (one-procedure?> :days-delay 0) ;; "hoof trim"
```

Once you have names, you can use the tables to populate a
database or whatever.

## Groups of names that satisfy constraints.

Let's say the `permitted??` relation/goal succeeds when a
particular procedure is permitted on a particular
animal. So, for example:

```clojure
(permitted?? "hoof trim" "hank") ;; succeed
(permitted?? "hoof trim" "dawn") ;; fail
```

Now we can ask for pairs that are permitted:

```clojure
(permitted?>) ;; [["hoof trim", "hank"], ["superovulation" "betty"]], etc.
(one-permitted?> :procedure "hoof trim") ;; ["hoof trim" "hank"], etc.
```

## Hierarchical data

The app from which these examples are drawn is largely
about *reservations*, which contain one or more  (permitted) procedure/animal pairs. I
want to show a "picture" of the "shape" of reservation I
want, and then let an engine fill in the blanks.

For example, the most common reservation used in the tests
is one group with
one procedure/animal pair. In many many cases, I don't
actually care which procedure and which animal I use. I want
to describe it like this:

```clojure
(reservation?> [- -] [- -])
```

... and get back a data structure something like this:

```clojure
([["hank" "hoof trim"] ["betty" "superovulation"]] ...)
```

Then a database-population routine (that would look
extremely like the one that already exists to add a
reservation from json data) would take over.

Sometimes you'd want constraints. Some of those can be
expressed in abbreviated form. Suppose I want two pairs, the
first containing Hank the Horse and some appropriate procedure, the
second containing a cow. That would look like this:

```clojure
(reservation?>  ["hank" -] [{:species :bovine} -])
```

I have a prototype of this working. The big question is
whether it's a plausible  [stunted framework](http://www.artima.com/weblogs/viewpost.jsp?thread=8826): one that's not so
universal that it's too hard to learn and too fiddly to work
with, but that doesn't require so much customization that
it's not worthwhile?

Another question: Logic programming is esoteric. The
framework aims to write *most* of a core.logic query for
you, but you need to provide some help. Can you use this
framework without having to be an expert in core.logic?

And: is Clojure a large enough language community? one that
builds the sort of apps that need sophisticated test data generation?

## License

Copyright © 2012 Brian Marick

Distributed under the Eclipse Public License, the same as Clojure.
