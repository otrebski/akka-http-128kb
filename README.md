# Akka HTTP JSON unmarshalling issue example


Let's take a look at following root example in [AkkaHttpMicroservice.scala](src/main/scala/AkkaHttpMicroservice.scala):

```scala
  val routes: Route = {

    val singleRoute = (post & entity(as[User])) { user =>
      complete {
        Response(s"Single user $user")
      }
    }
    val listRoute = (post & entity(as[List[User]])) { users =>
      complete {
        Response(s"List of users, size ${users.size}")

      }
    }
    
    path("u1") {
      listRoute ~
        singleRoute
    } ~ path("u2") {
      singleRoute ~
        listRoute
    }
    
  }

```
We have defined 2 URL's `u1` and `u2`. Both URL's are trying  to unmarshall JSON object or JSON array. Difference is in order.
`u1` tries to unmarshall JSON array first, `u2` tries to unmarshall JSON object first. If request is smaller than 128kB, both URL's works. If request is bigger than 128kB, only `u1` is able to unmarshall JSON array.
Error looks like this:
```http request
POST http://localhost:9000/u2
Content-Type: application/json
Accept: application/json

< ./biggerThan128kb.json

HTTP/1.1 400 Bad Request
Server: akka-http/10.0.11
Date: Fri, 19 Jan 2018 07:48:59 GMT
Content-Type: text/plain; charset=UTF-8
Content-Length: 71

The request content was malformed:
Object expected in field 'firstName'
```

On the other hand, sending request smaller than 128kb works fine:
```http request
POST http://localhost:9000/u2
Content-Type: application/json
Accept: application/json

HTTP/1.1 200 OK
Server: akka-http/10.0.11
Date: Fri, 19 Jan 2018 21:10:33 GMT
Content-Type: application/json
Content-Length: 35

{"body":"List of users, size 1914"}

```

Example requests are in [test.http](test.http) file (Intellij Rest client)