from micronaut.vaadin.annotation import UIScope


# tag::counter[]
@UIScope  # <1>
class VisitCounter:

    def __init__(self):
        self.count = 0

    def increment(self) -> int:
        self.count += 1
        return self.count
# end::counter[]
