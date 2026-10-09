# Assignment 3 - BPMN Process Models

Two BPMN 2.0 process models, each with swimlanes and a range of event types. Open the `.bpmn` files in [bpmn.io](https://demo.bpmn.io/), Camunda Modeler, or any BPMN 2.0 compatible tool.

## Files

| File | Process |
|------|---------|
| `Student Project Approval and Allocation.bpmn` | Student Project Approval & Allocation |
| `Logistics and Shipment Exception.bpmn` | Shipment Life Cycle (with exceptions) |

---

## 1. Student Project Approval & Allocation

A student team submits a project proposal. It is validated, screened, checked for similarity and evaluated by a committee. If approved, a faculty guide is allocated.

**Lanes:** Student / Team, Project Management System, Project Coordinator, Review Committee (incl. HoD), Faculty Guide

**Main flow:** Submit proposal -> Validate form & team rules -> Valid? -> Screen proposal -> Run similarity check -> Evaluate proposal -> Committee decision -> Check guide availability & workload -> Allocate guide -> Send allocation request -> Guide accepts -> Notify student -> Record allocation -> **Project allocated**

**Decisions**
- *Valid?* - No: proposal is returned to the student with an error list.
- *Committee decision?* - Approved: continue to guide allocation. Revise: sent back for revision. Rejected: proposal rejected.

**Events**
- Timer boundary event "Deadline missed" on *Submit project proposal*: team is flagged as late and notified.
- Non-interrupting timer boundary event "No decision in 7 days" (`P7D`) on *Evaluate proposal*: a reminder is sent to the committee.

**Task types used:** user, service, business rule, send

---

## 2. Logistics & Shipment Exception (Shipment Life Cycle)

A shipper books a shipment. It is picked up, moved through hubs, and delivered to the recipient, with handling for missing shippers, damaged parcels and absent recipients.

**Lanes:** Shipper / Customer, Control Tower / Dispatcher, Pickup Agent, Hub / Warehouse, Claims Department, Delivery Agent, Recipient

**Main flow:** Create booking -> Validate booking & calculate charges -> Schedule pickup & assign driver -> Collect parcel & scan -> **parallel split**
- Branch A: Hub sorting & line-haul (multi-instance sub-process, one instance per leg: sort parcel, line-haul to destination hub)
- Branch B: Update tracking -> Notify shipper

-> **parallel join** -> Dispatch & notify recipient -> Recipient available? -> Hand over parcel & capture proof of delivery -> Close shipment & generate invoice -> **Shipment delivered**

**Exceptions**
- Error boundary "Shipper absent" on *Collect parcel & scan*: booking is cancelled and a fee charged.
- Error boundary "Damage found" (`PARCEL_DAMAGED`) on the hub sub-process: a damage claim is handled by the Claims Department.
- *Recipient available?* - No: parcel is returned to the sender.

**Constructs used:** parallel gateways, exclusive gateway, multi-instance sub-process, error events, user, service, business rule, send and manual tasks

---

## Author

Hemendra - RA2411027010108
