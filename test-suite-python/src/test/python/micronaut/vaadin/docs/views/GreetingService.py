from jakarta.inject import Singleton


# tag::service[]
@Singleton
class GreetingService:

    def greet(self, name: str) -> str:
        return "Hello " + name
# end::service[]
